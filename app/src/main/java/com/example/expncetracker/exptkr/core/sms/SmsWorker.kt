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
            val success = processSms(body, address, timestamp)
            val status = if (success) "COMPLETE" else "UNPARSEABLE"
            kotlin.runCatching { db.rawSmsDao().updateStatus(timestamp.toString(), status) }
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            
            Logger.e("SmsWorker", "Error processing SMS: ${e.message}", e)
            kotlin.runCatching { db.rawSmsDao().updateStatus(timestamp.toString(), "FAILED") }
            
            when (e) {
                is NumberFormatException, 
                is IllegalArgumentException,
                is IllegalStateException -> Result.failure()
                else -> Result.retry()
            }
        }
    }

    private suspend fun processSms(body: String, address: String, timestamp: Long): Boolean {
        // 1. Save to RawSms table (Phase 1 Resilience)
        val rawSms = RawSmsEntity(
            smsId = timestamp.toString(), // Using timestamp as a simple ID for now if not available
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
            return false
        }

        // 3. Categorization
        val categoryName = categoryDetector.detect(parsedSms.merchant, parsedSms.type)
        
        // 4. Anomaly Detection (Phase 2)
        // Simple rule: Flag if amount > 20000 (Example threshold)
        val isAnomaly = parsedSms.amount.compareTo(java.math.BigDecimal(20000)) > 0
        val confidence = if (isAnomaly) 0.5f else 0.95f
        val status = if (isAnomaly) "NEEDS_REVIEW" else "COMPLETE"

        // 5. Idempotency Hash (Phase 1)
        val hash = SecurityUtils.calculateTransactionHash(parsedSms.amount, timestamp, parsedSms.merchant, address)

        // 6. Get or Create Account ID
        var accountId = db.accountDao().getAccountIdByName(parsedSms.bankName)
        if (accountId == null || accountId == 0L) {
            // Auto-create account if not found
            val newAccount = com.example.expncetracker.exptkr.data.db.entity.AccountEntity(
                name = parsedSms.bankName,
                balance = java.math.BigDecimal.ZERO,
                type = "SAVINGS",
                color = 0xFF3B82F6.toInt() // Default blue
            )
            accountId = db.accountDao().insertAccount(newAccount)
            Logger.d("SmsWorker", "Auto-created account for: ${parsedSms.bankName}")
        }

        // 7. Create Transaction
        val transaction = Transaction(
            smsId = "live_$timestamp", // Use a prefix to distinguish from imported SMS but keep it non-null
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
        return true
    }
}
