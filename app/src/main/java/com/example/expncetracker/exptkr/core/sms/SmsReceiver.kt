package com.example.expncetracker.exptkr.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.expncetracker.exptkr.core.common.Logger

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody
                val address = message.originatingAddress ?: "Unknown"
                val timestamp = message.timestampMillis

                Logger.d("SmsReceiver", "New SMS from $address: $body")

                // Trigger WorkManager for background processing
                val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                    .setInputData(workDataOf(
                        "body" to body,
                        "address" to address,
                        "timestamp" to timestamp
                    ))
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
