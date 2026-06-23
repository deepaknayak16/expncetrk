package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.parser.CategoryDetector
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.sms.SmsReader
import com.example.expncetracker.exptkr.data.db.dao.AccountDao 
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportSmsTransactionsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: ParserRegistry,
    private val categoryDetector: CategoryDetector,
    private val repository: TransactionRepository,
    private val accountDao: AccountDao
) {
    suspend fun execute() {
        // FETCH WIDE: Look back 30 days to ensure we don't miss anything 
        // regardless of what the DB says (DB handles duplicates via idempotencyHash)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val lastTimestampInDb = repository.getLatestSmsTimestamp()
        
        // Use the earlier of the two to be safe, but capped at 30 days lookback
        val fetchSince = if (lastTimestampInDb == 0L) thirtyDaysAgo else (lastTimestampInDb - 600000).coerceAtLeast(thirtyDaysAgo)
        
        Logger.d("ImportSmsTransactions", "Starting sync since timestamp: $fetchSince (30d ago was $thirtyDaysAgo)")
        
        val rawSmsList = withContext(Dispatchers.IO) {
            smsReader.fetchSmsSince(fetchSince)
        }
        Logger.d("ImportSmsTransactions", "Fetched ${rawSmsList.size} raw candidate messages")

        val parsedTransactions = rawSmsList.mapNotNull { raw ->
            val parsedSms = parserRegistry.parseSms(raw.address, raw.body, raw.timestamp) ?: return@mapNotNull null
            val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)
            val accountId = accountDao.getAccountIdByName(parsedSms.bankName) ?: 0L 

            val hash = com.example.expncetracker.exptkr.core.common.SecurityUtils.calculateTransactionHash(
                parsedSms.amount, raw.timestamp, parsedSms.merchant, raw.address
            )

            Transaction(
                smsId = raw.smsId,
                amount = parsedSms.amount,
                type = parsedSms.type,
                categoryName = categoryName,
                merchant = parsedSms.merchant,
                bankName = parsedSms.bankName,
                note = "Imported from SMS",
                timestamp = parsedSms.timestamp,
                accountId = accountId,
                idempotencyHash = hash,
                confidenceScore = 0.9f,
                parsingStatus = "COMPLETE"
            )
        }

        Logger.d("ImportSmsTransactions", "Total valid transactions parsed: ${parsedTransactions.size}")

        parsedTransactions.forEach { transaction ->
            try {
                repository.insertTransactionWithBalance(transaction)
            } catch (e: Exception) {
                Logger.e("ImportSmsTransactions", "Error inserting transaction: ${e.message}")
            }
        }

        // One-time cleanup of any legacy duplicates caused by previous hashing issues
        try {
            repository.cleanupDuplicates()
        } catch (e: Exception) {
            Logger.e("ImportSmsTransactions", "Cleanup failed: ${e.message}")
        }
    }
}
