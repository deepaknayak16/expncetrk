package com.example.expncetracker.exptkr.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expncetracker.exptkr.MainActivity
import com.example.expncetracker.exptkr.R
import androidx.core.content.ContextCompat
object AppNotificationManager {
    private const val CHANNEL_ID = "quick_actions_channel"
    private const val BILLS_CHANNEL_ID = "bill_reminders_channel"
    private const val NOTIFICATION_ID = 1001

    fun showBillReminderNotification(context: Context, merchantName: String, amount: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BILLS_CHANNEL_ID,
                "Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when bills are due"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "dashboard")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BILLS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            .setContentTitle("Bill Due Today!")
            .setContentText("₹$amount is due for $merchantName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(merchantName.hashCode(), notification)
    }

    fun showQuickActionsNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Actions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Provides quick access to add expenses and view budgets"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "add_transaction")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val addExpensePendingIntent = PendingIntent.getActivity(
            context, 0, addExpenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val viewBudgetsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "budget")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val viewBudgetsPendingIntent = PendingIntent.getActivity(
            context, 1, viewBudgetsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(
                ContextCompat.getColor(
                    context,
                    android.R.color.holo_blue_dark
                )
            ) // <-- Fixed closing parenthesis here
            .setContentTitle("Expense Tracker")
            .setContentText("Quickly manage your finances")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Add Expense", addExpensePendingIntent)
            .addAction(0, "View Budgets", viewBudgetsPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
