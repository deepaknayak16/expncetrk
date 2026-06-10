package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.model.TransactionDto
import com.example.expncetracker.exptkr.data.model.toDomain
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(): Boolean {
        val file = File(context.filesDir, Constants.BACKUP_FILE_NAME)
        if (!file.exists()) return false

        return try {
            val jsonStr = file.readText()
            val dtoList = Json.decodeFromString<List<TransactionDto>>(jsonStr)
            val transactions = dtoList.map { it.toDomain() }
            
            if (transactions.isNotEmpty()) {
                repository.clearAllTransactions()
                repository.insertTransactions(transactions)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
