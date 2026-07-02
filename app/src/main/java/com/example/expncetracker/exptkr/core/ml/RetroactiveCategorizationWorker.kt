package com.example.expncetracker.exptkr.core.ml

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RetroactiveCategorizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val mlEngine: HybridMlEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("RetroactiveWorker", "Starting retroactive re-categorization...")
        
        try {
            val transactions = transactionRepository.getRecentTransactions(500).first()
            val blindSpots = transactions.filter { 
                (it.categoryName.lowercase() == "others" || it.confidenceScore < 0.5f) &&
                !it.isCategoryManuallyCorrected
            }
            
            if (blindSpots.isEmpty()) {
                Logger.d("RetroactiveWorker", "No blind spots found.")
                return Result.success()
            }
            
            Logger.d("RetroactiveWorker", "Found ${blindSpots.size} blind spots to re-categorize")
            
            blindSpots.forEach { tx ->
                val result = mlEngine.infer(
                    merchantName = tx.merchant,
                    amount = tx.amount,
                    type = tx.type,
                    timestamp = tx.timestamp.toEpochMilli(),
                    smsBody = tx.rawSmsBody ?: ""
                )
                
                if (result.category != tx.categoryName && result.confidenceScore > 0.8f) {
                    transactionRepository.updateTransactionCategory(tx.id, result.category, result.confidenceScore)
                    Logger.d("RetroactiveWorker", "Re-categorized ${tx.merchant}: ${tx.categoryName} -> ${result.category}")
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            Logger.e("RetroactiveWorker", "Failed retroactive re-categorization", e)
            return Result.retry()
        }
    }

    companion object {
        fun run(context: Context) {
            val request = OneTimeWorkRequestBuilder<RetroactiveCategorizationWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "retroactive_categorization",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
