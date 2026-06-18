package com.example.expncetracker.exptkr.domain.usecase.sync

import android.content.Context
import com.example.expncetracker.exptkr.core.sync.GoogleDriveSyncManager
import com.example.expncetracker.exptkr.data.model.TransactionDto
import com.example.expncetracker.exptkr.data.model.toDomain
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreBackupFromGoogleDriveUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context,
    private val driveSyncManager: GoogleDriveSyncManager
) {
    suspend fun execute(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!driveSyncManager.isSignedIn()) {
                return@withContext Result.failure(Exception("Please sign in to Google first"))
            }

            val tempFile = File(context.cacheDir, "gdrive_restore_${System.currentTimeMillis()}.json")

            val downloadSuccess = driveSyncManager.downloadBackup(tempFile)

            if (!downloadSuccess) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Failed to download backup from Google Drive"))
            }

            val jsonStr = tempFile.readText()
            val dtoList = Json.decodeFromString<List<TransactionDto>>(jsonStr)
            val transactions = dtoList.map { it.toDomain() }

            // WHY: If the JSON is empty or bad, we must STOP before touching the database.
//      The "finally" guarantees the temp file is always deleted, even on crash.
            try {
                if (transactions.isEmpty()) {
                    return@withContext Result.failure(Exception("Backup file is empty — nothing to restore"))
                }
                repository.replaceTransactions(transactions)
                Result.success("Data restored from Google Drive successfully")
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
