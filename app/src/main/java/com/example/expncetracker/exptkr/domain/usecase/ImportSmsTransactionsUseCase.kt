package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.parser.CategoryDetector
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.core.sms.SmsReader
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import javax.inject.Inject

class ImportSmsTransactionsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val parserRegistry: ParserRegistry,
    private val categoryDetector: CategoryDetector,
    private val repository: TransactionRepository
) {
    suspend fun execute() {
        // IMPORTANT: We use getLatestSmsTimestamp to ignore sample data dates
        val lastTimestamp = repository.getLatestSmsTimestamp()
        val rawSmsList = smsReader.fetchSmsSince(lastTimestamp)

        val parsedTransactions = rawSmsList.mapNotNull { raw ->
            val parsedSms = parserRegistry.parseSms(raw.address, raw.body, raw.timestamp) ?: return@mapNotNull null
            val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)

            Transaction(
                smsId = raw.smsId,
                amount = parsedSms.amount,
                type = parsedSms.type,
                categoryName = categoryName,
                merchant = parsedSms.merchant,
                bankName = parsedSms.bankName,
                note = "Imported from SMS",
                timestamp = parsedSms.timestamp
            )
        }

        if (parsedTransactions.isNotEmpty()) {
            repository.insertTransactions(parsedTransactions)
        }
    }
}
