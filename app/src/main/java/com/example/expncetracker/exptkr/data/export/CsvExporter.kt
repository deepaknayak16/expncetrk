package com.example.expncetracker.exptkr.data.export

import android.content.Context
import com.example.expncetracker.exptkr.domain.model.TransactionType
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
                    if (transaction.isRecurring) return@filter false
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

            java.io.FileOutputStream(outputFile).use { fos ->
                // Write UTF-8 BOM for Excel compatibility
                fos.write(0xEF)
                fos.write(0xBB)
                fos.write(0xBF)

                java.io.OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    // Write CSV header
                    writer.appendLine("ID,Date,Time,Category,Type,Description,Bank,Amount")

                    // Write transactions
                    transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                        val date = transaction.timestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val time = transaction.timestamp.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
                        val category = escapeCsvField(transaction.categoryName)
                        val type = when(transaction.type) {
                            TransactionType.CREDIT -> "Income"
                            TransactionType.DEBIT -> "Expense"
                            TransactionType.TRANSFER -> "Transfer"
                            TransactionType.LEND -> "Lent"
                            TransactionType.BORROW -> "Borrowed"
                        }
                        val description = escapeCsvField(transaction.merchant)
                        val bank = escapeCsvField(transaction.bankName)
                        val amount = String.format(Locale.US, "%.2f", transaction.amount)

                        writer.appendLine("${transaction.id},$date,$time,$category,$type,$description,$bank,$amount")
                    }
                    writer.flush()
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escapes a CSV field by wrapping in quotes if it contains special characters and prevents CSV Formula Injection.
     */
    private fun escapeCsvField(field: String): String {
        var cleanField = field
        // Prevent CSV Formula Injection by prepending a single quote if it starts with dynamic formula triggers
        if (cleanField.startsWith("=") || cleanField.startsWith("+") || cleanField.startsWith("-") || 
            cleanField.startsWith("@") || cleanField.startsWith("\t") || cleanField.startsWith("\r")) {
            cleanField = "'$cleanField"
        }

        return if (cleanField.contains(",") || cleanField.contains("\"") || cleanField.contains("\n") || cleanField.contains("\r")) {
            "\"${cleanField.replace("\"", "\"\"")}\""
        } else {
            cleanField
        }
    }
}
