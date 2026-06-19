package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.model.TransactionDto
import com.example.expncetracker.exptkr.data.model.toDomain
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(): Boolean {
        val file = File(context.filesDir, Constants.BACKUP_FILE_NAME)
        if (!file.exists()) return false

        return try {
            val jsonStr = file.readText()
            val dtoList = Json.decodeFromString<List<TransactionDto>>(jsonStr)
            val transactions = dtoList.map { it.toDomain() }

            if (transactions.isEmpty()) {
                return false
            }

            repository.replaceTransactions(transactions)

            // FIX: Recalculate balances using the same formula as runtime (CREDIT/BORROW +, DEBIT/LEND -)
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
