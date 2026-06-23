package com.example.expncetracker.exptkr.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase,
    private val repository: TransactionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            performSync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun performSync() {
        // Phase 4: Cloud Sync Logic
        // 1. Fetch transactions where syncStatus == PENDING
        // 2. Push to cloud API
        // 3. Update local syncStatus to SYNCED
        // TODO: Implement actual API calls here
    }

    companion object {
        private const val SYNC_WORK_NAME = "cloud_sync_work"

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
