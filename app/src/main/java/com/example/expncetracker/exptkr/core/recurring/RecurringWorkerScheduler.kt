package com.example.expncetracker.exptkr.core.recurring

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecurringWorkerScheduler {
    
    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        // 1. Detective: Runs every 24 hours to detect patterns
        val detectionRequest = PeriodicWorkRequestBuilder<PatternDetectionWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "pattern_detection_work",
            ExistingPeriodicWorkPolicy.KEEP,
            detectionRequest
        )

        // 2. Guardian: Runs every 12 hours (to catch 8 AM roughly)
        val reminderRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "bill_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
