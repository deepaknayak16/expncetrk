package com.example.expncetracker.exptkr.core.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.SecurityUtils
import com.example.expncetracker.exptkr.core.parser.CategoryDetector
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime

@HiltWorker
class SmsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase,
    private val repository: TransactionRepository,
    private val parserRegistry: ParserRegistry,
    private val categoryDetector: CategoryDetector
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val body = inputData.getString("body") ?: return Result.failure()
        val address = inputData.getString("address") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", 0L)

        return try {
            processSms(body, address, timestamp)
            Result.success()
        } catch (e: Exception) {
            Logger.e("SmsWorker", "Error processing SMS: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun processSms(body: String, address: String, timestamp: Long) {
        // 1. Save to RawSms table (Phase 1 Resilience)
        val rawSms = RawSmsEntity(
            smsId = timestamp, // Using timestamp as a simple ID for now if not available
            body = body,
            address = address,
            timestamp = timestamp,
            parsingStatus = "PROCESSING"
        )
        db.rawSmsDao().insertRawSmsList(listOf(rawSms))

        // 2. Filter & Parse
        val parsedSms = parserRegistry.parseSms(address, body, timestamp)
        if (parsedSms == null) {
            Logger.d("SmsWorker", "SMS from $address could not be parsed: $body")
            return
        }

        // 3. Categorization
        val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)
        
        // 4. Anomaly Detection (Phase 2)
        // Simple rule: Flag if amount > 20000 (Example threshold)
        val isAnomaly = parsedSms.amount > 20000.0
        val confidence = if (isAnomaly) 0.5f else 0.95f
        val status = if (isAnomaly) "NEEDS_REVIEW" else "COMPLETE"

        // 5. Idempotency Hash (Phase 1)
        val hash = SecurityUtils.generateHash("${parsedSms.amount}${timestamp}${parsedSms.merchant}${address}")

        // 6. Get Account ID
        val accountId = db.accountDao().getAccountIdByName(parsedSms.bankName) ?: 0L

        // 7. Create Transaction
        val transaction = Transaction(
            smsId = timestamp,
            amount = parsedSms.amount,
            type = parsedSms.type,
            categoryName = categoryName,
            merchant = parsedSms.merchant,
            bankName = parsedSms.bankName,
            note = if (isAnomaly) "Anomaly: Needs Review" else "Auto-parsed from SMS",
            timestamp = parsedSms.timestamp,
            accountId = accountId,
            idempotencyHash = hash,
            confidenceScore = confidence,
            parsingStatus = status
        )

        // 8. Atomic Write with Balance Update
        repository.insertTransactionWithBalance(transaction)
        
        Logger.d("SmsWorker", "Successfully processed and saved transaction from $address")
    }
}
