package com.example.expncetracker.exptkr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.expncetracker.exptkr.core.workers.RecurringTransactionWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        com.example.expncetracker.exptkr.core.workers.RecurringTransactionWorker.schedule(this)
        com.example.expncetracker.exptkr.core.workers.BudgetAlertWorker.schedule(this)
    }
}
