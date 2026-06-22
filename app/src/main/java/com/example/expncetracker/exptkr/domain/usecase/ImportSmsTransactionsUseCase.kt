package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.parser.CategoryDetector
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.sms.SmsReader
import com.example.expncetracker.exptkr.data.db.dao.AccountDao 
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import javax.inject.Inject

class ImportSmsTransactionsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: ParserRegistry,
    private val categoryDetector: CategoryDetector,
    private val repository: TransactionRepository,
    private val accountDao: AccountDao
) {
    suspend fun execute() {
        // Fetch since last timestamp - subtract 5 minutes to avoid missing messages 
        // that arrived during the last sync but with a slightly earlier timestamp.
        val lastTimestamp = (repository.getLatestSmsTimestamp() - 300000).coerceAtLeast(0L)
        
        Logger.d("ImportSmsTransactions", "Starting sync since timestamp: $lastTimestamp")
        
        val rawSmsList = smsReader.fetchSmsSince(lastTimestamp)
        Logger.d("ImportSmsTransactions", "Fetched ${rawSmsList.size} raw candidate messages")

        val parsedTransactions = rawSmsList.mapNotNull { raw ->
            val parsedSms = parserRegistry.parseSms(raw.address, raw.body, raw.timestamp) ?: return@mapNotNull null
            val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)
            val accountId = accountDao.getAccountIdByName(parsedSms.bankName) ?: 0L 

            val hash = com.example.expncetracker.exptkr.core.common.SecurityUtils.generateHash(
                "${parsedSms.amount}${parsedSms.timestamp}${parsedSms.merchant}${raw.address}"
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
    }
}
