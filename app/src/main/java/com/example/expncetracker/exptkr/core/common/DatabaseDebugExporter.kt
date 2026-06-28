package com.example.expncetracker.exptkr.core.common

import android.content.Context
import com.example.expncetracker.exptkr.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseDebugExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    fun exportUnencryptedDb(): String? {
        return try {
            val tempDir = File(context.cacheDir, "debug_db")
            if (!tempDir.exists()) tempDir.mkdirs()
            
            val outputFile = File(tempDir, "unencrypted_debug.db")
            if (outputFile.exists()) outputFile.delete()

            val supportDb = db.openHelper.writableDatabase
            
            try {
                // SQLCipher export commands:
                // 1. Attach a new empty database (no password)
                supportDb.execSQL("ATTACH DATABASE '${outputFile.absolutePath}' AS plaintext KEY ''")
                
                // 2. Export everything to it
                // MUST use query() (which is rawQuery in SupportSQLiteDatabase) for sqlcipher_export 
                // to avoid "Queries can be performed using SQLiteDatabase query or rawQuery methods only"
                supportDb.query("SELECT sqlcipher_export('plaintext')").use { cursor ->
                    cursor.moveToFirst()
                }
                
                Logger.d("DbExport", "Database exported successfully to: ${outputFile.absolutePath}")
                outputFile.absolutePath
            } finally {
                // 3. Detach it (always try to detach if it was attached)
                try {
                    supportDb.execSQL("DETACH DATABASE plaintext")
                } catch (e: Exception) {
                    // Ignore detach errors if it wasn't attached
                }
            }
        } catch (e: Exception) {
            Logger.e("DbExport", "Failed to export unencrypted DB: ${e.message}", e)
            null
        }
    }
}
