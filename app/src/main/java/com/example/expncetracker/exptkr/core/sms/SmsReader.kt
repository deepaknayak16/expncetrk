package com.example.expncetracker.exptkr.core.sms

import android.content.Context
import android.net.Uri
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun fetchSmsSince(timestamp: Long): List<RawSmsEntity> {
        val smsList = mutableListOf<RawSmsEntity>()
        
        if (!SmsPermissionManager.hasPermissions(context)) {
            Logger.e("SmsReader", "SMS permission not granted. Cannot fetch SMS.")
            return smsList
        }
        Logger.d("SmsReader", "SMS permissions granted. Fetching SMS since $timestamp")

        val uris = listOf(Uri.parse("content://sms/inbox"))

        for (uri in uris) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf("_id", "address", "body", "date"),
                    "date > ?",
                    arrayOf(timestamp.toString()),
                    "date DESC"
                )?.use { cursor ->
                    Logger.d("SmsReader", "Cursor count for $uri: ${cursor.count}")
                    
                    val idIndex = cursor.getColumnIndexOrThrow("_id")
                    val addressIndex = cursor.getColumnIndexOrThrow("address")
                    val bodyIndex = cursor.getColumnIndexOrThrow("body")
                    val dateIndex = cursor.getColumnIndexOrThrow("date")

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val address = cursor.getString(addressIndex) ?: ""
                        val body = cursor.getString(bodyIndex) ?: ""
                        
                        val isBankBySender = Constants.BANK_SENDERS.any { prefix ->
                            address.uppercase().contains(prefix.uppercase()) 
                        }

                        val isBankByBody = Constants.BANK_SENDERS.any { prefix ->
                            body.uppercase().contains(prefix.uppercase())
                        }

                        val isWallet = Constants.WALLET_SENDERS.any { prefix ->
                            address.uppercase().contains(prefix.uppercase()) || body.uppercase().contains(prefix.uppercase())
                        }
                        val allAllowedSenders = Constants.BANK_SENDERS + Constants.WALLET_SENDERS
                        val isAllowedSender = allAllowedSenders.any { prefix ->
                            address.uppercase().contains(prefix.uppercase()) || body.uppercase().contains(prefix.uppercase())
                        }

                        val isRelevant = isBankBySender || isBankByBody || isWallet

                        if (isRelevant) {
                            smsList.add(
                                RawSmsEntity(
                                    smsId = id,
                                    address = address,
                                    body = body,
                                    timestamp = cursor.getLong(dateIndex)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("SmsReader", "Error fetching SMS from $uri: ${e.message}", e)
            }
        }
        
        Logger.d("SmsReader", "Total bank SMS messages found: ${smsList.size}")
        return smsList
    }
}
