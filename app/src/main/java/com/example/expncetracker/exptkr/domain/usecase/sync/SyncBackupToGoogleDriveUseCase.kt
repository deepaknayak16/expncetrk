package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.core.sync.GoogleDriveSyncManager
import com.example.expncetracker.exptkr.data.model.toDto
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for syncing backup data to Google Drive.
 */
@Singleton
class SyncBackupToGoogleDriveUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context,
    private val driveSyncManager: GoogleDriveSyncManager
) {
    /**
     * Execute the sync to Google Drive.
     * @param accountName The Google account email to use for authentication.
     * @return A message indicating success or failure.
     */
    suspend fun execute(accountName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Initialize Google Drive service
            driveSyncManager.initializeDriveService(accountName)

            // Get all transactions
            val txList = repository.getAllTransactions().first()
            if (txList.isEmpty()) {
                return@withContext Result.failure(Exception("No transactions to backup"))
            }

            // Convert to DTO and serialize
            val dtoList = txList.map { it.toDto() }
            val jsonString = Json.encodeToString(dtoList)

            // Create temporary backup file
            val backupFile = File(context.cacheDir, "gdrive_backup_${System.currentTimeMillis()}.json")
            backupFile.writeText(jsonString)

            // Upload to Google Drive
            val fileId = driveSyncManager.uploadBackup(backupFile)
            
            // Clean up temp file
            backupFile.delete()

            if (fileId != null) {
                Result.success("Backup synced to Google Drive successfully")
            } else {
                Result.failure(Exception("Failed to upload backup to Google Drive"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
