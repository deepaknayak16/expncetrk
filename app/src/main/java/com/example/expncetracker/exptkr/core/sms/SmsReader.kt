package com.example.expncetracker.exptkr.core.sms

import android.content.Context
import android.net.Uri
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun fetchSmsSince(timestamp: Long): List<RawSmsEntity> {
        val smsList = mutableListOf<RawSmsEntity>()
        
        android.util.Log.d("SmsReader", "Checking SMS permissions...")
        if (!SmsPermissionManager.hasPermissions(context)) {
            android.util.Log.e("SmsReader", "SMS permission not granted. Cannot fetch SMS.")
            return smsList
        }
        android.util.Log.d("SmsReader", "SMS permissions granted. Fetching SMS since $timestamp")

        val uris = listOf(
            Uri.parse("content://sms/inbox"),
            Uri.parse("content://sms/")
        )

        val processedIds = mutableSetOf<Long>()

        for (uri in uris) {
            try {
                android.util.Log.d("SmsReader", "Querying URI: $uri")
                context.contentResolver.query(
                    uri,
                    arrayOf("_id", "address", "body", "date"),
                    "date > ?",
                    arrayOf(timestamp.toString()),
                    "date DESC"
                )?.use { cursor ->
                    android.util.Log.d("SmsReader", "Cursor count for $uri: ${cursor.count}")
                    if (cursor.count == 0) {
                        android.util.Log.d("SmsReader", "No SMS found since timestamp $timestamp for $uri")
                    }
                    
                    val idIndex = cursor.getColumnIndexOrThrow("_id")
                    val addressIndex = cursor.getColumnIndexOrThrow("address")
                    val bodyIndex = cursor.getColumnIndexOrThrow("body")
                    val dateIndex = cursor.getColumnIndexOrThrow("date")

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        if (processedIds.contains(id)) continue
                        
                        val address = cursor.getString(addressIndex) ?: ""
                        val body = cursor.getString(bodyIndex) ?: ""
                        
                        android.util.Log.d("SmsReader", "Processing SMS from: $address")
                        android.util.Log.d("SmsReader", "Body: ${body.take(100)}...")

                        val isBankBySender = Constants.BANK_SENDERS.any { prefix ->
                            address.uppercase().contains(prefix.uppercase()) 
                        }

                        val isBankByBody = Constants.BANK_SENDERS.any { prefix ->
                            body.uppercase().contains(prefix.uppercase())
                        }

                        val isBank = isBankBySender //|| isBankByBody

                        android.util.Log.d("SmsReader", "Is Bank: $isBank")

                        if (isBank) {
                            android.util.Log.d("SmsReader", "Found bank SMS from - Sender match: $isBankBySender, Body match: $isBankByBody")
                            smsList.add(
                                RawSmsEntity(
                                    smsId = id,
                                    address = address,
                                    body = body,
                                    timestamp = cursor.getLong(dateIndex)
                                )
                            )
                            processedIds.add(id)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SmsReader", "Error fetching SMS from $uri: ${e.message}", e)
            }
        }
        
        android.util.Log.d("SmsReader", "Total bank SMS messages found: ${smsList.size}")
        return smsList
    }
}
