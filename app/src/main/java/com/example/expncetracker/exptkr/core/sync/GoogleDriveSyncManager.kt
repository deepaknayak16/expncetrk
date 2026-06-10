package com.example.expncetracker.exptkr.core.sync

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File

/**
 * Manages Google Drive synchronization for backup and restore operations.
 */
class GoogleDriveSyncManager(private val context: Context) {

    private var driveService: Drive? = null

    /**
     * Initialize the Google Drive service with the provided account name.
     * @param accountName The Google account email to use for authentication.
     */
    fun initializeDriveService(accountName: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).setSelectedAccountName(accountName)

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("ExpenseTracker")
            .build()
    }

    /**
     * Upload a backup file to Google Drive.
     * @param backupFile The local backup file to upload.
     * @return The ID of the uploaded file on Google Drive, or null if failed.
     */
    suspend fun uploadBackup(backupFile: File): String? {
        return try {
            val drive = driveService ?: throw IllegalStateException("Drive service not initialized")

            // Check if file already exists
            val existingFileId = findExistingBackupFile()

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "expense_tracker_backup.json"
                mimeType = "application/json"
            }

            val mediaContent = com.google.api.client.http.FileContent(
                "application/json",
                backupFile
            )

            val fileId = if (existingFileId != null) {
                // Update existing file
                drive.files().update(existingFileId, fileMetadata, mediaContent)
                    .execute()
                    .id
            } else {
                // Create new file
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                    .id
            }

            fileId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Download backup file from Google Drive.
     * @param destinationFile The local file to save the backup to.
     * @return True if download was successful, false otherwise.
     */
    suspend fun downloadBackup(destinationFile: File): Boolean {
        return try {
            val drive = driveService ?: throw IllegalStateException("Drive service not initialized")

            val fileId = findExistingBackupFile()
                ?: throw IllegalStateException("No backup file found on Google Drive")

            val outputStream = destinationFile.outputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Find the existing backup file on Google Drive.
     * @return The file ID if found, null otherwise.
     */
    private fun findExistingBackupFile(): String? {
        val drive = driveService ?: return null

        val query = "name='expense_tracker_backup.json' and mimeType='application/json' and trashed=false"
        
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    /**
     * Check if the user is signed in to Google Drive.
     * @return True if signed in, false otherwise.
     */
    fun isSignedIn(): Boolean {
        return driveService != null
    }

    /**
     * Sign out from Google Drive.
     */
    fun signOut() {
        driveService = null
    }
}
