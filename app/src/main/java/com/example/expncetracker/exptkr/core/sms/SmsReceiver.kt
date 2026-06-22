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

            if (messages.isNullOrEmpty()) return

            // FIX 1: Handle Multi-part SMS
            // Concatenate all message bodies into a single string
            val completeBody = messages.joinToString("") { it.messageBody ?: "" }

            // Take the address and timestamp from the first part
            val address = messages[0].originatingAddress ?: "Unknown"
            val timestamp = messages[0].timestampMillis

            Logger.d("SmsReceiver", "New SMS from $address: $completeBody")

            // FIX 2: Prevent duplicate processing using Unique Work
            // Create a unique ID based on sender and timestamp so if the broadcast fires twice,
            // WorkManager ignores the second request.
            val uniqueWorkName = "sms_${address}_$timestamp"

            val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                .setInputData(
                    workDataOf(
                        "body" to completeBody,
                        "address" to address,
                        "timestamp" to timestamp
                    )
                )
                .build()

            // Using enqueueUniqueWork ensures this exact SMS is only processed once
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName,
                    androidx.work.ExistingWorkPolicy.KEEP, // If already queued, keep the old one
                    workRequest
                )
        }
    }
}