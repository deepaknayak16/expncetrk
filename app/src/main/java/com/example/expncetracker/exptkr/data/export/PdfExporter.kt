package com.example.expncetracker.exptkr.data.export

import android.content.Context
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    @ApplicationContext private val context: Context,
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
            val outputDir = File(context.getExternalFilesDir(null), "reports")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, fileName)

            // Create PDF document
            val writer = PdfWriter(FileOutputStream(outputFile))
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)

            // Add title
            val title = Paragraph("Expense Report")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(title)

            // Add date range
            val dateRangeText = buildString {
                append("Period: ")
                append(startDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Beginning")
                append(" to ")
                append(endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Present")
            }
            document.add(Paragraph(dateRangeText)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setMarginBottom(20f))

            // Add summary
            val totalIncome = transactions.filter { it.type.name == "INCOME" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }
            val balance = totalIncome - totalExpense

            document.add(Paragraph("Summary")
                .setFontSize(16f)
                .setBold()
                .setMarginTop(20f)
                .setMarginBottom(10f))

            document.add(Paragraph("Total Income: ₹${String.format("%.2f", totalIncome)}"))
            document.add(Paragraph("Total Expense: ₹${String.format("%.2f", totalExpense)}"))
            document.add(Paragraph("Balance: ₹${String.format("%.2f", balance)}")
                .setBold()
                .setMarginBottom(20f))

            // Add transactions table
            document.add(Paragraph("Transaction Details")
                .setFontSize(16f)
                .setBold()
                .setMarginTop(20f)
                .setMarginBottom(10f))

            val table = Table(floatArrayOf(3f, 4f, 3f, 3f, 3f))

            // Table header
            listOf("Date", "Category", "Type", "Description", "Amount").forEach { header ->
                table.addHeaderCell(header)
            }

            // Table data
            transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
                table.addCell(transaction.timestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                table.addCell(transaction.category.name)
                table.addCell(transaction.type.name)
                table.addCell(transaction.merchant.take(20))
                val amountText = if (transaction.type.name == "EXPENSE") {
                    "-₹${String.format("%.2f", transaction.amount)}"
                } else {
                    "+₹${String.format("%.2f", transaction.amount)}"
                }
                table.addCell(amountText)
            }

            document.add(table)

            // Add footer
            document.add(Paragraph("Generated on ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setMarginTop(30f))

            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Table.addHeaderCell(text: String) {
        addCell(
            Cell()
                .add(Paragraph(text))
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
        )
    }

    private fun Table.addCell(text: String) {
        addCell(
            Cell()
                .add(Paragraph(text))
                .setTextAlignment(TextAlignment.LEFT)
        )
    }
}