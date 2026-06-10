package com.example.expncetracker.exptkr.domain.usecase

import android.content.Context
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.model.toDto
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val repository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(): String {
        val txList = repository.getAllTransactions().first()
        val dtoList = txList.map { it.toDto() }
        
        val jsonString = Json.encodeToString(dtoList)

        val file = File(context.filesDir, Constants.BACKUP_FILE_NAME)
        file.writeText(jsonString)
        return file.absolutePath
    }
}
