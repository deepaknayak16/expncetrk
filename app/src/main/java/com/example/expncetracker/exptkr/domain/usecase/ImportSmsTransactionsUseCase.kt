package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.parser.CategoryDetector
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.sms.SmsReader
import com.example.expncetracker.exptkr.data.db.dao.AccountDao // FIXED: added import
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import javax.inject.Inject

class ImportSmsTransactionsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: ParserRegistry,
    private val categoryDetector: CategoryDetector,
    private val repository: TransactionRepository, // FIXED: added comma
    private val accountDao: AccountDao
) {
    suspend fun execute() {
        val lastTimestamp = repository.getLatestSmsTimestamp()
        val rawSmsList = smsReader.fetchSmsSince(lastTimestamp)

        val parsedTransactions = rawSmsList.mapNotNull { raw ->
            val parsedSms = parserRegistry.parseSms(raw.address, raw.body, raw.timestamp) ?: return@mapNotNull null
            val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)
            val accountId = accountDao.getAccountIdByName(parsedSms.bankName) ?: 0L // FIXED: was parsed.bankName

            Transaction(
                smsId = raw.smsId,
                amount = parsedSms.amount,
                type = parsedSms.type,
                categoryName = categoryName,
                merchant = parsedSms.merchant,
                bankName = parsedSms.bankName,
                note = "Imported from SMS",
                timestamp = parsedSms.timestamp,
                accountId = accountId
            )
        }

        if (parsedTransactions.isNotEmpty()) {
            repository.insertTransactions(parsedTransactions)
        }
    }
}
