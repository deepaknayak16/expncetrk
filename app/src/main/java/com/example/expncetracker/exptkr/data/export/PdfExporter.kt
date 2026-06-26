package com.example.expncetracker.exptkr.data.export

import android.content.Context
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * PDF Export utility for generating expense reports.
 */
@Singleton
class PdfExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Exports transactions to a PDF file.
     * @param startDate Start date for the report (inclusive)
     * @param endDate End date for the report (inclusive)
     * @param fileName Name of the output PDF file
     * @return File path of the generated PDF
     */
    suspend fun exportToPdf(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        fileName: String = "expense_report_${LocalDate.now()}.pdf"
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

            // Create PDF document with proper resource management
            FileOutputStream(outputFile).use { fos ->
                val document = Document(PdfDocument(PdfWriter(fos)), PageSize.A4)
                document.use { doc ->
                    // Add title
                    val title = Paragraph("Expense Report")
                        .setFontSize(20f)
                        .simulateBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f)
                    doc.add(title)

                    // Add date range
                    val dateRangeText = buildString {
                        append("Period: ")
                        append(startDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Beginning")
                        append(" to ")
                        append(endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Present")
                    }
                    doc.add(Paragraph(dateRangeText)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12f)
                        .setMarginBottom(20f))

                    // Add summary
                    val totalIncome = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
                    val totalExpense = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                    val balance = totalIncome - totalExpense

                    doc.add(Paragraph("Summary")
                        .setFontSize(16f)
                        .simulateBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f))

                    doc.add(Paragraph("Total Income: ₹${String.format(Locale.US, "%.2f", totalIncome)}"))
                    doc.add(Paragraph("Total Expense: ₹${String.format(Locale.US, "%.2f", totalExpense)}"))
                    doc.add(Paragraph("Balance: ₹${String.format(Locale.US, "%.2f", balance)}")
                        .simulateBold()
                        .setMarginBottom(20f))

                    // Add transactions table
                    doc.add(Paragraph("Transaction Details")
                        .setFontSize(16f)
                        .simulateBold()
                        .setMarginTop(20f)
                        .setMarginBottom(10f))

                    val table = Table(floatArrayOf(3f, 4f, 3f, 3f, 3f))

                    // Table header
                    listOf("Date", "Category", "Type", "Description", "Amount").forEach { header ->
                        table.addStyledHeaderCell(header)
                    }

                    // Table data
                    transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                        table.addStyledCell(transaction.timestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        table.addStyledCell(transaction.categoryName.lowercase().replaceFirstChar { it.uppercase() })
                        table.addStyledCell(when(transaction.type) {
                            TransactionType.CREDIT -> "Income"
                            TransactionType.DEBIT -> "Expense"
                            TransactionType.TRANSFER -> "Transfer"
                            TransactionType.LEND -> "Lent"
                            TransactionType.BORROW -> "Borrowed"
                        })
                        table.addStyledCell(transaction.merchant.take(20))
                        val amountText = when (transaction.type) {
                            TransactionType.DEBIT -> "-₹${String.format(Locale.US, "%.2f", transaction.amount)}"
                            TransactionType.CREDIT -> "+₹${String.format(Locale.US, "%.2f", transaction.amount)}"
                            TransactionType.TRANSFER -> "₹${String.format(Locale.US, "%.2f", transaction.amount)}"
                            TransactionType.LEND -> "↗ ₹${String.format(Locale.US, "%.2f", transaction.amount)}"
                            TransactionType.BORROW -> "↙ ₹${String.format(Locale.US, "%.2f", transaction.amount)}"
                        }
                        table.addStyledCell(amountText)
                    }

                    doc.add(table)

                    // Add footer
                    doc.add(Paragraph("Generated on ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10f)
                        .setMarginTop(30f))
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Table.addStyledHeaderCell(text: String) {
        addCell(
            Cell()
                .add(Paragraph(text))
                .simulateBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
        )
    }

    private fun Table.addStyledCell(text: String) {
        addCell(
            Cell()
                .add(Paragraph(text))
                .setTextAlignment(TextAlignment.LEFT)
        )
    }
}
