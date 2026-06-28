package com.example.expncetracker.exptkr.core.common

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

import java.util.Locale

object SecurityUtils {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private val passphrase = Any()
    fun getOrCreatePassphrase(context: Context): ByteArray = synchronized(passphrase) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val storedPassphrase = sharedPreferences.getString(KEY_PASSPHRASE, null)
        return if (storedPassphrase != null) {
            Base64.decode(storedPassphrase, Base64.DEFAULT)
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            val encodedPassphrase = Base64.encodeToString(newPassphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(KEY_PASSPHRASE, encodedPassphrase).apply()
            newPassphrase
        }
    }

    fun generateHash(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun calculateTransactionHash(amount: java.math.BigDecimal, timestamp: Long, merchant: String, sender: String): String {
        // Use a consistent format to ensure the hash is identical across different parts of the app
        // 1. Force Locale.US for decimal consistency
        val amountStr = amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString()
        
        // 2. Normalize merchant (remove extra spaces, uppercase)
        val normalizedMerchant = merchant.trim().uppercase()
        
        // 3. Normalize sender (take last 6 chars to ignore AD- or AX- prefixes)
        val cleanSender = sender.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val normalizedSender = if (cleanSender.length > 6) cleanSender.takeLast(6) else cleanSender
        
        // 4. Round timestamp to nearest second to avoid millisecond drift in some SMS providers
        val normalizedTimestamp = kotlin.math.round(timestamp / 1000.0).toLong() * 1000
        
        return generateHash("$amountStr|$normalizedTimestamp|$normalizedMerchant|$normalizedSender")
    }
}
