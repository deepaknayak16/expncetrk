package com.example.expncetracker.exptkr.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.expncetracker.exptkr.domain.usecase.CheckBudgetAlertsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkBudgetAlertsUseCase: CheckBudgetAlertsUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkBudgetAlertsUseCase.execute()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // FIX BUG-023: Only retry on transient errors (here we check attempt count)
            if (runAttemptCount < 3 && isTransientError(e)) Result.retry() else Result.failure()
        }
    }

    private fun isTransientError(e: Exception): Boolean {
        return e is android.database.sqlite.SQLiteDatabaseLockedException || 
               e is java.io.IOException
    }

    companion object {
        private const val WORK_NAME = "budget_alerts_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
