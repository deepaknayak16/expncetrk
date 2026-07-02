package com.example.expncetracker.exptkr.core.sms

import android.content.Context
import android.net.Uri
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.common.SecurityUtils
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun fetchSmsSince(timestamp: Long): List<RawSmsEntity> {
        val smsList = mutableListOf<RawSmsEntity>()
        
        // FIX BUG-013: Add a hard limit to avoid OOM on first install (90 days)
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        val effectiveTimestamp = timestamp.coerceAtLeast(ninetyDaysAgo)

        if (!SmsPermissionManager.hasPermissions(context)) {
            Logger.e("SmsReader", "SMS permission not granted. Cannot fetch SMS.")
            return smsList
        }
        Logger.d("SmsReader", "SMS permissions granted. Fetching SMS since $effectiveTimestamp")

        val uris = listOf(Uri.parse("content://sms/inbox"))

        for (uri in uris) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf("_id", "address", "body", "date"),
                    "date > ?",
                    arrayOf(effectiveTimestamp.toString()),
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
                        val cleanSender = address.uppercase().replace(Regex("^[A-Z]{2}-"), "")
                        
                        val isBankBySender = Constants.BANK_SENDERS.any { prefix ->
                            cleanSender.startsWith(prefix.uppercase()) 
                        }

                        val isWalletBySender = Constants.WALLET_SENDERS.any { prefix ->
                            cleanSender.startsWith(prefix.uppercase())
                        }
                        
                        // Fallback: If it's a short code (not a phone number) and contains currency keywords
                        val isShortCode = address.length < 15 && address.any { it.isLetter() }
                        val containsCurrency = body.contains("Rs", ignoreCase = true) || 
                                             body.contains("INR", ignoreCase = true) ||
                                             body.contains("₹")
                        
                        // FIX #H10: Require an actual transaction keyword to filter out promotional SMS
                        val txKeywords = "(?:debited|spent|withdrawn|transferred|paid|sent|credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)"
                        val containsKeyword = body.contains(txKeywords.toRegex(RegexOption.IGNORE_CASE))

                        val isRelevant = isBankBySender || isWalletBySender || (isShortCode && containsCurrency && containsKeyword)

                        if (isRelevant) {
                            Logger.d("SmsReader", "Found relevant SMS from $address: ${body.take(20)}...")
                            // FIX BUG-6: Use consistent date-invariant hash
                            val smsHash = com.example.expncetracker.exptkr.core.common.HashingUtil.generateSmsHash(address, body)
                            smsList.add(
                                RawSmsEntity(
                                    smsId = smsHash,
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
