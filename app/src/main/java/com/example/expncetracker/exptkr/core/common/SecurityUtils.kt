package com.example.expncetracker.exptkr.core.common

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object SecurityUtils {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

    fun getOrCreatePassphrase(context: Context): ByteArray {
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
}
