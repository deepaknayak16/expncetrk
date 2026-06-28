package com.example.expncetracker.exptkr.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.expncetracker.exptkr.core.common.HashingUtil
import com.example.expncetracker.exptkr.core.common.Logger

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isNullOrEmpty()) return

            // FIX 1: Handle Multi-part SMS
            val completeBody = messages.joinToString("") { it.messageBody ?: "" }
            val address = messages.firstNotNullOfOrNull { it.originatingAddress } ?: "Unknown"
            val timestamp = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

            // PHASE 1: Idempotency Gate
            // Generate hash here at the entrance to prevent duplicate work queuing
            val smsHash = HashingUtil.generateSmsHash(address, completeBody)

            Logger.d("SmsReceiver", "New SMS from $address, Hash: $smsHash")

            val uniqueWorkName = "sms_$smsHash"

            val workRequest = OneTimeWorkRequestBuilder<SmsWorker>()
                .setInputData(
                    workDataOf(
                        "body" to completeBody,
                        "address" to address,
                        "timestamp" to timestamp,
                        "smsHash" to smsHash
                    )
                )
                .build()

            // Using enqueueUniqueWork with the hash as the name
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName,
                    androidx.work.ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}