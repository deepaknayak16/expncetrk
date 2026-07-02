package com.example.expncetracker.exptkr.core.sync

import android.content.Context
import android.util.Log
import com.example.expncetracker.exptkr.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Drive synchronization for backup and restore operations.
 */
@Singleton
class GoogleDriveSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleSignInClient: GoogleSignInClient
) {

    @Volatile
    private var driveService: Drive? = null
    private val lock = Any()

    /**
     * Get the Drive service, initializing it if necessary.
     */
    fun getDriveService(): Drive? = synchronized(lock) {
        if (driveService == null) {
            initializeDriveService()
        }
        return driveService
    }

    /**
     * Initialize the Google Drive service using the last signed-in account.
     * @return True if initialized successfully, false otherwise.
     */
    fun initializeDriveService(): Boolean = synchronized(lock) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        // FIX BUG-004: Create a new NetHttpTransport each time to avoid permanent shutdown issues
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
                parents = listOf("appDataFolder")
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
            if (BuildConfig.DEBUG) android.util.Log.e("GDriveSync", "Upload failed: ${e.message}")
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
            if (BuildConfig.DEBUG) android.util.Log.e("GDriveSync", "Download failed: ${e.message}")
            false
        }
    }

    private fun findExistingBackupFile(): String? {
        val drive = driveService ?: return null

        val query = "name='expense_tracker_backup.json' and mimeType='application/json' and trashed=false"

        val result = drive.files().list()
            .setQ(query)
            .setSpaces("appDataFolder")
            .setFields("files(id, name)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    suspend fun signOut() {
        synchronized(lock) {
            driveService = null
            // Don't shut down transport - just clear the service reference
            // Re-initialization logic in initializeDriveService handles it if it was shutdown
        }
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            Log.e("GDriveSync", "Sign out failed", e)
        }
    }
}
