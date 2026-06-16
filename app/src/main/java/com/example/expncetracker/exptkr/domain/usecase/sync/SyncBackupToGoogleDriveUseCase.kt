package com.example.expncetracker.exptkr.domain.usecase.sync

import android.content.Context
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

@Singleton
class SyncBackupToGoogleDriveUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context,
    private val driveSyncManager: GoogleDriveSyncManager
) {
    suspend fun execute(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!driveSyncManager.isSignedIn()) {
                return@withContext Result.failure(Exception("Please sign in to Google first"))
            }

            val txList = repository.getAllTransactions().first()
            if (txList.isEmpty()) {
                return@withContext Result.failure(Exception("No transactions to backup"))
            }

            val dtoList = txList.map { it.toDto() }
            val jsonString = Json.encodeToString(dtoList)

            val backupFile = File(context.cacheDir, "gdrive_backup_${System.currentTimeMillis()}.json")
            backupFile.writeText(jsonString)

            val fileId = driveSyncManager.uploadBackup(backupFile)
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
