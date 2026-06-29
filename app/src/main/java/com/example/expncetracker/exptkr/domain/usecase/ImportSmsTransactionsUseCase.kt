package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.HashingUtil
import com.example.expncetracker.exptkr.core.ml.HybridMlEngine
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.sms.SmsReader
import com.example.expncetracker.exptkr.core.common.Constants.DEFAULT_ACCOUNT_COLOR
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import javax.inject.Inject

class ImportSmsTransactionsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: ParserRegistry,
    private val mlEngine: HybridMlEngine,
    private val repository: TransactionRepository,
    private val accountDao: AccountDao
) {
    suspend fun execute() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val lastTimestampInDb = repository.getLatestSmsTimestamp()
        
        val fetchSince = if (lastTimestampInDb == 0L) thirtyDaysAgo else (lastTimestampInDb - 600000).coerceAtLeast(thirtyDaysAgo)
        
        Logger.d("ImportSmsTransactions", "Starting sync since timestamp: $fetchSince")
        
        val rawSmsList = withContext(Dispatchers.IO) {
            smsReader.fetchSmsSince(fetchSince)
        }
        Logger.d("ImportSmsTransactions", "Fetched ${rawSmsList.size} raw candidate messages")

        rawSmsList.forEach { raw ->
            // FIX: True Idempotency Hash check at start of loop
            val smsHash = HashingUtil.generateSmsHash(raw.address, raw.body)
            
            if (repository.doesHashExist(smsHash)) {
                Logger.d("ImportSmsTransactions", "Duplicate skipped: $smsHash")
                return@forEach
            }

            val parsedSms = parserRegistry.parseSms(raw.address, raw.body, raw.timestamp) ?: return@forEach

            // 4. ML-based Inference
            val mlResult = mlEngine.infer(
                merchantName = parsedSms.merchant,
                amount = parsedSms.amount,
                type = parsedSms.type,
                timestamp = parsedSms.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                smsBody = raw.body
            )

            var accountId = accountDao.getAccountIdByName(parsedSms.bankName.trim())
                ?: parsedSms.bankName.trim().substringBefore(" ").takeIf { it.isNotBlank() }?.let {
                    accountDao.getAccountIdByName(it)
                }

            if (accountId == null || accountId == 0L) {
                val isLiquid = !parsedSms.bankName.contains("EPFO", ignoreCase = true) && 
                              !parsedSms.bankName.contains("Post Office", ignoreCase = true) &&
                              !parsedSms.bankName.contains("DOPBNK", ignoreCase = true)
                
                val newAccount = AccountEntity(
                    name = parsedSms.bankName,
                    balance = java.math.BigDecimal.ZERO,
                    type = if (isLiquid) "SAVINGS" else "INVESTMENT",
                    color = DEFAULT_ACCOUNT_COLOR,
                    isLiquid = isLiquid
                )
                accountId = accountDao.insertAccount(newAccount)
            }

            val status = if (parsedSms.isIntentOnly) "REMINDER" else mlResult.parsingStatus

            val transaction = Transaction(
                smsId = smsHash,
                amount = parsedSms.amount,
                type = parsedSms.type,
                categoryName = mlResult.category,
                merchant = parsedSms.merchant,
                bankName = parsedSms.bankName,
                note = when (mlResult.anomalyLevel) {
                    com.example.expncetracker.exptkr.core.ml.AmountAnomalyScorer.AnomalyLevel.NORMAL -> "Imported from SMS"
                    else -> "Anomaly detected: ${mlResult.anomalyLevel}. Expected ~₹${mlResult.expectedAmount ?: "N/A"}"
                },
                timestamp = parsedSms.timestamp,
                accountId = accountId,
                idempotencyHash = smsHash,
                confidenceScore = mlResult.confidenceScore,
                parsingStatus = status,
                rawSmsBody = raw.body,
                smsFingerprint = smsHash,
                recurringState = mlResult.recurringState,
                cleanMerchantName = mlResult.cleanMerchantName // FIX BUG-GEN-05
            )

            try {
                if (transaction.parsingStatus == "REMINDER") {
                    repository.insertTransaction(transaction)
                } else {
                    repository.insertTransactionWithBalance(transaction)
                }
            } catch (e: Exception) {
                Logger.e("ImportSmsTransactions", "Error inserting transaction: ${e.message}")
            }
        }
    }
}
