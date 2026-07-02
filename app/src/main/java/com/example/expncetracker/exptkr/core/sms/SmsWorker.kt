package com.example.expncetracker.exptkr.core.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expncetracker.exptkr.core.common.Constants.DEFAULT_ACCOUNT_COLOR
import com.example.expncetracker.exptkr.core.common.HashingUtil
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.core.common.SecurityUtils
import com.example.expncetracker.exptkr.core.ml.HybridMlEngine
import com.example.expncetracker.exptkr.core.parser.ParserRegistry
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import com.example.expncetracker.exptkr.data.db.entity.SmsQuarantineEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@HiltWorker
class SmsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase,
    private val repository: TransactionRepository,
    private val parserRegistry: ParserRegistry,
    private val mlEngine: HybridMlEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val body = inputData.getString("body") ?: return Result.failure()
        val address = inputData.getString("address") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", 0L)
        val smsHash = inputData.getString("smsHash") ?: HashingUtil.generateSmsHash(address, body)

        return try {
            // 1. PHASE 2: Idempotency Check (DB Gate)
            if (db.transactionDao().doesHashExist(smsHash)) {
                Logger.d("SmsWorker", "Duplicate SMS skipped via Hash: $smsHash")
                return Result.success()
            }

            val success = processSms(smsHash, body, address, timestamp)
            if (!success) {
                // FIX BUG-7: Update raw_sms status to SKIPPED
                db.rawSmsDao().updateStatus(smsHash, "SKIPPED")

                // Quarantine for unparseable data
                val quarantine = SmsQuarantineEntity(
                    smsId = smsHash,
                    body = body,
                    address = address,
                    timestamp = timestamp,
                    errorReason = "UNPARSEABLE"
                )
                db.smsQuarantineDao().insert(quarantine)
            }
            Result.success()
        } catch (e: Exception) {
            // FIX BUG-026: Rethrow CancellationException BEFORE any other logic
            if (e is kotlinx.coroutines.CancellationException) throw e
            
            Logger.e("SmsWorker", "Error processing SMS: ${e.message}", e)
            
            // Phase 8: Quarantine on ANY failure
            val quarantine = SmsQuarantineEntity(
                smsId = smsHash,
                body = body,
                address = address,
                timestamp = timestamp,
                errorReason = "ERROR: ${e.message}"
            )
            db.smsQuarantineDao().insert(quarantine)
            
            Result.success() // Don't retry, it's quarantined
        }
    }

    private suspend fun processSms(smsId: String, body: String, address: String, timestamp: Long): Boolean {
        // 2. Save to RawSms table
        val rawSms = RawSmsEntity(
            smsId = smsId,
            body = body,
            address = address,
            timestamp = timestamp,
            parsingStatus = "PROCESSING"
        )
        db.rawSmsDao().insertRawSmsList(listOf(rawSms))

        // 3. Filter & Parse (Includes Fix 4: Informational Traps)
        val parsedSms = parserRegistry.parseSms(address, body, timestamp)
        if (parsedSms == null) {
            // If parsedSms is null, it might be an informational trap (Phase 8 Exception)
            // or just unparseable. The caller handles quarantine.
            return false
        }

        // 4. ML-based Inference (Categorization + Anomaly + Recurring)
        val mlResult = mlEngine.infer(
            merchantName = parsedSms.merchant,
            amount = parsedSms.amount,
            type = parsedSms.type,
            timestamp = parsedSms.timestamp.toEpochMilli(),
            smsBody = body
        )
        
        val status = if (parsedSms.isIntentOnly) "REMINDER" else mlResult.parsingStatus

        // 6. Idempotency Hash (Re-use the robust sender|body hash)
        val hash = smsId

        // 7. Get or Create Account ID
        val trimmedBankName = parsedSms.bankName.trim()
        val lastDigits = trimmedBankName.takeLast(4).filter { it.isDigit() }
        val bankPrefix = trimmedBankName.substringBefore(" ").trim()

        var accountId = db.accountDao().getAccountIdByName(trimmedBankName)
            ?: (if (lastDigits.length == 4) db.accountDao().findAccountIdByDigits(bankPrefix, lastDigits) else null)
            ?: bankPrefix.takeIf { it.isNotBlank() }?.let {
                db.accountDao().getAccountIdByName(it)
            }

        if ((accountId == null) || (accountId == 0L)) {
            val isLiquid = !parsedSms.bankName.contains("EPFO", ignoreCase = true) && 
                          !parsedSms.bankName.contains("Post Office", ignoreCase = true) &&
                          !parsedSms.bankName.contains("DOPBNK", ignoreCase = true)
            
            val newAccount = com.example.expncetracker.exptkr.data.db.entity.AccountEntity(
                name = parsedSms.bankName,
                balance = java.math.BigDecimal.ZERO,
                type = if (isLiquid) "SAVINGS" else "INVESTMENT",
                color = DEFAULT_ACCOUNT_COLOR,
                isLiquid = isLiquid
            )
            accountId = db.accountDao().insertAccount(newAccount)
        }

        // 8. Create Transaction
        val transaction = Transaction(
            smsId = smsId,
            amount = parsedSms.amount,
            type = parsedSms.type,
            categoryName = mlResult.category,
            merchant = parsedSms.merchant,
            bankName = parsedSms.bankName,
            note = when (mlResult.anomalyLevel) {
                com.example.expncetracker.exptkr.core.ml.AmountAnomalyScorer.AnomalyLevel.NORMAL -> "Auto-parsed from SMS"
                else -> "Anomaly detected: ${mlResult.anomalyLevel}. Expected ~₹${mlResult.expectedAmount ?: "N/A"}"
            },
            timestamp = parsedSms.timestamp,
            accountId = accountId,
            idempotencyHash = hash,
            confidenceScore = mlResult.confidenceScore,
            parsingStatus = status,
            rawSmsBody = body,
            smsFingerprint = smsId,
            recurringState = mlResult.recurringState,
            cleanMerchantName = mlResult.cleanMerchantName // FIX BUG-GEN-05: Use standardized name from ML engine
        )

        // 9. Atomic Write with Balance Update
        if (parsedSms.isIntentOnly) {
            repository.insertTransaction(transaction)
        } else {
            repository.insertTransactionWithBalance(transaction, parsedSms.accountBalance)
        }
        
        db.rawSmsDao().updateStatus(smsId, "COMPLETE")
        Logger.d("SmsWorker", "Successfully processed and saved transaction from $address")
        return true
    }
}
