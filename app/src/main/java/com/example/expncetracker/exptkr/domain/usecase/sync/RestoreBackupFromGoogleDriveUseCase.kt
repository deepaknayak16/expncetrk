package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.model.TransactionDto
import com.example.expncetracker.exptkr.data.model.toDomain
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RestoreBackupFromGoogleDriveUseCase @Inject constructor(
    private val driveSyncManager: GoogleDriveSyncManager, // Inject manager instead
    private val driveService: Drive,
    private val repository: TransactionRepository,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileList = driveService.files().list()
                .setQ("name='${Constants.BACKUP_FILE_NAME}' and trashed=false")
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val file = fileList.files.firstOrNull() ?: return@withContext false

            val content = driveService.files().get(file.id).executeMediaAsInputStream()
            val jsonStr = content.bufferedReader().use { it.readText() }

            val dtoList = Json.decodeFromString<List<TransactionDto>>(jsonStr)
            val transactions = dtoList.map { it.toDomain() }

            if (transactions.isEmpty()) {
                return@withContext false
            }

            repository.replaceTransactions(transactions)

            // FIX: Recalculate balances after restore
            val accounts = accountDao.getAllAccounts().first()
            accounts.forEach { account ->
                val newBalance = transactionDao.calculateNetBalanceByAccount(account.id)
                accountDao.updateAccount(account.copy(balance = newBalance))
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
