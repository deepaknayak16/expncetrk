package com.example.expncetracker.exptkr.core.recurring

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import com.example.expncetracker.exptkr.domain.model.TransactionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

@HiltWorker
class PatternDetectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("PatternDetectionWorker", "Starting background pattern detection...")
        
        return try {
            detectRecurringPatterns()
            Result.success()
        } catch (e: Exception) {
            Logger.e("PatternDetectionWorker", "Error detecting patterns: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun detectRecurringPatterns() {
        val transactions = db.transactionDao().getAllTransactionsSync()
        
        val expenseGroups = transactions
            .filter { it.type == TransactionType.DEBIT.name }
            .groupBy { it.cleanMerchantName ?: cleanMerchant(it.merchant) }

        for (entry in expenseGroups) {
            val cleanName = entry.key
            val list = entry.value
            if (list.size >= 2) {
                analyzeGroup(cleanName, list)
            }
        }
    }

    private fun cleanMerchant(merchant: String): String {
        val raw = merchant.uppercase()
            .replace(Regex("^[A-Z]{2}-"), "") // Strip bank prefixes like AD-, AX-
            .replace(Regex("[^A-Z ]"), "") // Strip numbers and special chars, KEEP spaces
            .trim()
        
        val words = raw.split(" ")
            .filter { it.length >= 3 } // Ignore very short noise words
        
        return when {
            words.isEmpty() -> "UNKNOWN"
            words[0] == "BMTC" -> "BMTC" // Special case for bus
            words[0].length >= 5 -> words[0] // e.g. "NETFLIX", "AIRTEL"
            words.size >= 2 -> "${words[0]} ${words[1]}" // e.g. "HDFC BANK"
            else -> words[0]
        }
    }

    private suspend fun analyzeGroup(cleanName: String, transactions: List<TransactionEntity>) {
        val amounts = transactions.map { it.amount }
        val avg = amounts.reduce { acc, bigDecimal -> acc + bigDecimal }.divide(BigDecimal(amounts.size), 2, java.math.RoundingMode.HALF_UP)
        
        // Check price stability (Variance < 15%)
        val isStable = amounts.all { 
            val diff = it.subtract(avg).abs()
            val percent = if (avg > BigDecimal.ZERO) diff.divide(avg, 4, java.math.RoundingMode.HALF_UP).toDouble() else 1.0
            percent < 0.15
        }

        val lastTx = transactions.maxByOrNull { it.timestamp } ?: return
        val nextDueDate = Instant.ofEpochMilli(lastTx.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusMonths(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val existingTemplate = db.recurringTemplateDao().getTemplateByCleanMerchant(cleanName)
        
        if (existingTemplate == null) {
            val state = if (isStable) RecurringState.ACTIVE.name else RecurringState.PENDING_CONFIRM.name
            val template = RecurringTemplateEntity(
                merchantName = lastTx.merchant,
                cleanMerchantName = cleanName,
                amount = avg,
                category = lastTx.category,
                frequency = "MONTHLY",
                nextDueDate = nextDueDate,
                state = state,
                lastDetectedDate = System.currentTimeMillis(),
                confidenceScore = if (isStable) 0.9f else 0.5f
            )
            db.recurringTemplateDao().insert(template)
            
            // Moment 2: Link old SMS records to this template
            updateOldRecords(cleanName, state)
            
            Logger.d("PatternDetectionWorker", "Created new template for $cleanName (State: $state)")
        } else if (existingTemplate.state == RecurringState.ACTIVE.name || existingTemplate.state == RecurringState.PENDING_CONFIRM.name) {
            val updatedTemplate = existingTemplate.copy(
                amount = avg,
                lastDetectedDate = System.currentTimeMillis(),
                confidenceScore = if (isStable) 0.95f else 0.6f
            )
            db.recurringTemplateDao().update(updatedTemplate)
        }
    }

    private suspend fun updateOldRecords(cleanName: String, state: String) {
        val transactions = db.transactionDao().getAllTransactionsSync()
        val toUpdate = transactions.filter { cleanMerchant(it.merchant) == cleanName }
        
        for (tx in toUpdate) {
            db.transactionDao().updateTransaction(tx.copy(
                recurringState = state,
                cleanMerchantName = cleanName
            ))
        }
    }
}
