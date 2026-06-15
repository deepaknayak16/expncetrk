package com.example.expncetracker.exptkr.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expncetracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for when you reach your budget limits"
            }
            notificationManager.createNotificationChannel(budgetChannel)
        }
    }

    fun showBudgetAlert(categoryName: String, spentPercentage: Int) {
        val title = "Budget Alert: $categoryName"
        val message = "You've reached $spentPercentage% of your budget for $categoryName."
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(categoryName.hashCode() + System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_BUDGET = "budget_alerts_channel"
    }
}
