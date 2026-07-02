package com.example.expncetracker.exptkr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.expncetracker.exptkr.core.ml.RuleSeeder
import com.example.expncetracker.exptkr.core.workers.RecurringTransactionWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var ruleSeeder: RuleSeeder

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Seed ML rules from assets
        CoroutineScope(Dispatchers.IO).launch {
            ruleSeeder.seedIfNeeded()
        }

        com.example.expncetracker.exptkr.core.workers.RecurringTransactionWorker.schedule(this)
        com.example.expncetracker.exptkr.core.workers.BudgetAlertWorker.schedule(this)
        com.example.expncetracker.exptkr.core.recurring.RecurringWorkerScheduler.scheduleAll(this)
        // Ensure any previous zombie sync worker is cancelled
        com.example.expncetracker.exptkr.core.sync.SyncWorker.cancel(this)
        // com.example.expncetracker.exptkr.core.sync.SyncWorker.schedule(this) // TODO: Enable when sync logic is implemented
    }
}
