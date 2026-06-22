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
                        val date = cursor.getLong(dateIndex)
                        
                        // Broaden relevance check: check if it looks like a transactional SMS 
                        // even if the sender is not in the list yet
                        val isBankBySender = Constants.BANK_SENDERS.any { prefix ->
                            address.uppercase().contains(prefix.uppercase()) 
                        }

                        val isWalletBySender = Constants.WALLET_SENDERS.any { prefix ->
                            address.uppercase().contains(prefix.uppercase())
                        }
                        
                        // Fallback: If it's a short code (not a phone number) and contains currency keywords
                        val isShortCode = address.length < 15 && address.any { it.isLetter() }
                        val containsCurrency = body.contains("Rs", ignoreCase = true) || 
                                             body.contains("INR", ignoreCase = true) ||
                                             body.contains("₹")

                        val isRelevant = isBankBySender || isWalletBySender || (isShortCode && containsCurrency)

                        if (isRelevant) {
                            Logger.d("SmsReader", "Found relevant SMS from $address: ${body.take(20)}...")
                            smsList.add(
                                RawSmsEntity(
                                    smsId = id,
                                    address = address,
                                    body = body,
                                    timestamp = date
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
