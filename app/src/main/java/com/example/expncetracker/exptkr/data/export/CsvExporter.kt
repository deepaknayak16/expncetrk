package com.example.expncetracker.exptkr.data.export

import android.content.Context
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * CSV Export utility for generating expense reports that can be opened in Excel/Google Sheets.
 */
@Singleton
class CsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Exports transactions to a CSV file.
     * @param startDate Start date for the report (inclusive)
     * @param endDate End date for the report (inclusive)
     * @param fileName Name of the output CSV file
     * @return File path of the generated CSV
     */
    suspend fun exportToCsv(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        fileName: String = "expense_report_${LocalDate.now()}.csv"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Fetch transactions from repository
            val transactions = transactionRepository.getAllTransactions().first()
                .filter { transaction ->
                    val txDate = transaction.timestamp.toLocalDate()
                    when {
                        startDate != null && endDate != null ->
                            !txDate.isBefore(startDate) && !txDate.isAfter(endDate)
                        startDate != null -> !txDate.isBefore(startDate)
                        endDate != null -> !txDate.isAfter(endDate)
                        else -> true
                    }
                }

            // Create output directory
            val externalDir = context.getExternalFilesDir(null)
                ?: context.filesDir  // fall back to internal storage
            val outputDir = File(externalDir, "reports")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, fileName)

            FileWriter(outputFile).use { writer ->
                // Write CSV header
                writer.appendLine("ID,Date,Time,Category,Type,Description,Bank,Amount")

                // Write transactions
                transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                    val date = transaction.timestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val time = transaction.timestamp.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
                    val category = transaction.category.name
                    val type = when(transaction.type) {
                        TransactionType.CREDIT -> "Income"
                        TransactionType.DEBIT -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                    }
                    val description = escapeCsvField(transaction.merchant)
                    val bank = escapeCsvField(transaction.bankName)
                    val amount = String.format(Locale.US, "%.2f", transaction.amount)

                    writer.appendLine("${transaction.id},$date,$time,$category,$type,$description,$bank,$amount")
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escapes a CSV field by wrapping in quotes if it contains special characters.
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
