package com.example.expncetracker.exptkr.domain.usecase

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

/**
 * Use case for restoring backup data from Google Drive.
 */
@Singleton
class RestoreBackupFromGoogleDriveUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context,
    private val driveSyncManager: GoogleDriveSyncManager
) {
    /**
     * Execute the restore from Google Drive.
     * @param accountName The Google account email to use for authentication.
     * @return A message indicating success or failure.
     */
    suspend fun execute(accountName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Initialize Google Drive service
            driveSyncManager.initializeDriveService(accountName)

            // Create temporary file to store downloaded backup
            val tempFile = File(context.cacheDir, "gdrive_restore_${System.currentTimeMillis()}.json")

            // Download from Google Drive
            val downloadSuccess = driveSyncManager.downloadBackup(tempFile)
            
            if (!downloadSuccess) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Failed to download backup from Google Drive"))
            }

            // Read and parse the backup file
            val jsonStr = tempFile.readText()
            val dtoList = Json.decodeFromString<List<TransactionDto>>(jsonStr)
            val transactions = dtoList.map { it.toDomain() }

            // Clear existing data and restore
            if (transactions.isNotEmpty()) {
                repository.clearAllTransactions()
                repository.insertTransactions(transactions)
            }

            // Clean up temp file
            tempFile.delete()

            Result.success("Data restored from Google Drive successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
