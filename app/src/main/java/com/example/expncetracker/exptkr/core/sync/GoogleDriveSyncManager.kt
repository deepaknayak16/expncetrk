package com.example.expncetracker.exptkr.core.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages Google Drive synchronization for backup and restore operations.
 */
class GoogleDriveSyncManager(private val context: Context) {

    private var driveService: Drive? = null

    /**
     * Initialize the Google Drive service using the last signed-in account.
     * @return True if initialized successfully, false otherwise.
     */
    fun initializeDriveService(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("ExpenseTracker")
            .build()

        return true
    }

    /**
     * Upload a backup file to Google Drive.
     * @param backupFile The local backup file to upload.
     * @return The ID of the uploaded file on Google Drive, or null if failed.
     */
    suspend fun uploadBackup(backupFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: if (initializeDriveService()) driveService else null
            if (drive == null) throw IllegalStateException("Drive service not initialized")

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
                drive.files().update(existingFileId, fileMetadata, mediaContent)
                    .execute()
                    .id
            } else {
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                    .id
            }

            fileId
        } catch (e: Exception) {
            android.util.Log.e("GDriveSync", "Upload failed: ${e.message}")
            null
        }
    }

    /**
     * Download backup file from Google Drive.
     * @param destinationFile The local file to save the backup to.
     * @return True if download was successful, false otherwise.
     */
    suspend fun downloadBackup(destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: if (initializeDriveService()) driveService else null
            if (drive == null) throw IllegalStateException("Drive service not initialized")

            val fileId = findExistingBackupFile()
                ?: throw IllegalStateException("No backup file found on Google Drive")

            destinationFile.outputStream().use { outputStream ->
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("GDriveSync", "Download failed: ${e.message}")
            false
        }
    }

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

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun signOut() {
        driveService = null
    }
}
