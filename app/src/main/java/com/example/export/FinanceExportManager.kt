package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BudgetEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.TransactionEntity
import com.example.util.CurrencyManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val reportDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    /**
     * Generates a native Android PDF Document for financial reporting and tax compliance.
     */
    fun generatePdfReport(
        context: Context,
        transactions: List<TransactionEntity>,
        subscriptions: List<SubscriptionEntity>,
        budgets: List<BudgetEntity>,
        baseCurrency: String,
        periodTitle: String = "All Time"
    ): Uri {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 DPI)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(6, 78, 59) // Emerald dark
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(100, 116, 139)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.rgb(30, 41, 59)
        }
        val amountIncomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(16, 185, 129)
        }
        val amountExpensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(239, 68, 68)
        }

        // 1. Header Banner
        paint.color = Color.rgb(209, 250, 229) // Emerald light background banner
        canvas.drawRect(0f, 0f, 595f, 70f, paint)

        canvas.drawText("FINANCIAL STATEMENT & TAX REPORT", 32f, 34f, titlePaint)
        canvas.drawText("Generated on ${reportDateFormat.format(Date())} | Period: $periodTitle | Base Currency: $baseCurrency", 32f, 52f, subTitlePaint)

        // 2. Summary Metrics Cards
        var totalIncome = 0.0
        var totalExpense = 0.0
        var totalTaxDeductible = 0.0

        transactions.forEach {
            val converted = CurrencyManager.convert(it.amount, it.currency, baseCurrency)
            if (it.type == "INCOME") {
                totalIncome += converted
            } else {
                totalExpense += converted
                if (it.isTaxDeductible) {
                    totalTaxDeductible += converted
                }
            }
        }
        val netSavings = totalIncome - totalExpense

        var curY = 95f

        // Metric Card 1: Income
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(32f, curY, 155f, curY + 45f, 6f, 6f, paint)
        subTitlePaint.textSize = 8f
        canvas.drawText("TOTAL INCOME", 42f, curY + 16f, subTitlePaint)
        headerPaint.textSize = 12f
        headerPaint.color = Color.rgb(16, 185, 129)
        canvas.drawText(CurrencyManager.formatAmount(totalIncome, baseCurrency), 42f, curY + 34f, headerPaint)

        // Metric Card 2: Expense
        canvas.drawRoundRect(165f, curY, 288f, curY + 45f, 6f, 6f, paint)
        canvas.drawText("TOTAL EXPENSE", 175f, curY + 16f, subTitlePaint)
        headerPaint.color = Color.rgb(239, 68, 68)
        canvas.drawText(CurrencyManager.formatAmount(totalExpense, baseCurrency), 175f, curY + 34f, headerPaint)

        // Metric Card 3: Net Balance
        canvas.drawRoundRect(298f, curY, 421f, curY + 45f, 6f, 6f, paint)
        canvas.drawText("NET SAVINGS", 308f, curY + 16f, subTitlePaint)
        headerPaint.color = if (netSavings >= 0) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
        canvas.drawText(CurrencyManager.formatAmount(netSavings, baseCurrency), 308f, curY + 34f, headerPaint)

        // Metric Card 4: Tax Deductible
        canvas.drawRoundRect(431f, curY, 563f, curY + 45f, 6f, 6f, paint)
        canvas.drawText("TAX DEDUCTIBLE", 441f, curY + 16f, subTitlePaint)
        headerPaint.color = Color.rgb(59, 130, 246) // Blue
        canvas.drawText(CurrencyManager.formatAmount(totalTaxDeductible, baseCurrency), 441f, curY + 34f, headerPaint)

        curY += 65f

        // 3. Tax Compliance Highlights Section
        paint.color = Color.rgb(238, 242, 255)
        canvas.drawRoundRect(32f, curY, 563f, curY + 38f, 6f, 6f, paint)
        headerPaint.color = Color.rgb(67, 56, 202)
        headerPaint.textSize = 10f
        canvas.drawText("TAX COMPLIANCE SUMMARY", 42f, curY + 16f, headerPaint)
        subTitlePaint.color = Color.rgb(79, 70, 229)
        subTitlePaint.textSize = 8.5f
        val deductibleCount = transactions.count { it.isTaxDeductible }
        canvas.drawText(
            "$deductibleCount tax-deductible items registered. Eligible deductions total ${CurrencyManager.formatAmount(totalTaxDeductible, baseCurrency)}.",
            42f,
            curY + 28f,
            subTitlePaint
        )

        curY += 55f

        // 4. Transactions Table
        headerPaint.color = Color.rgb(30, 41, 59)
        headerPaint.textSize = 12f
        canvas.drawText("Transaction Log & Records", 32f, curY, headerPaint)
        curY += 14f

        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(32f, curY, 563f, curY + 20f, paint)
        headerPaint.textSize = 9f
        canvas.drawText("DATE", 38f, curY + 13f, headerPaint)
        canvas.drawText("TITLE & NOTES", 110f, curY + 13f, headerPaint)
        canvas.drawText("CATEGORY", 290f, curY + 13f, headerPaint)
        canvas.drawText("TAX STATUS", 400f, curY + 13f, headerPaint)
        canvas.drawText("AMOUNT", 490f, curY + 13f, headerPaint)

        curY += 24f

        val maxRows = 24
        val displayTx = transactions.take(maxRows)

        displayTx.forEachIndexed { idx, tx ->
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(32f, curY - 3f, 563f, curY + 13f, paint)
            }
            bodyPaint.textSize = 8.5f
            bodyPaint.color = Color.rgb(71, 85, 105)
            canvas.drawText(dateFormat.format(Date(tx.date)), 38f, curY + 8f, bodyPaint)

            val titleWithNotes = if (tx.notes.isNotBlank()) {
                val full = "${tx.title} (${tx.notes})"
                if (full.length > 32) full.take(30) + "…" else full
            } else {
                if (tx.title.length > 32) tx.title.take(30) + "…" else tx.title
            }
            bodyPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(titleWithNotes, 110f, curY + 8f, bodyPaint)

            bodyPaint.color = Color.rgb(71, 85, 105)
            canvas.drawText(tx.category, 290f, curY + 8f, bodyPaint)

            val taxText = if (tx.isTaxDeductible) "Deductible (${tx.taxCategory.ifBlank { "Yes" }})" else "Standard"
            bodyPaint.color = if (tx.isTaxDeductible) Color.rgb(59, 130, 246) else Color.rgb(148, 163, 184)
            canvas.drawText(taxText, 400f, curY + 8f, bodyPaint)

            val isInc = tx.type == "INCOME"
            val sign = if (isInc) "+" else "-"
            val amtPaint = if (isInc) amountIncomePaint else amountExpensePaint
            canvas.drawText("$sign${CurrencyManager.formatAmount(tx.amount, tx.currency)}", 490f, curY + 8f, amtPaint)

            curY += 16f
        }

        if (transactions.size > maxRows) {
            subTitlePaint.textSize = 8f
            canvas.drawText("… and ${transactions.size - maxRows} more entries. Export CSV for full comprehensive ledger.", 32f, curY + 8f, subTitlePaint)
        }

        // Footer
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(32f, 810f, 563f, 810f, paint)
        subTitlePaint.textSize = 7.5f
        canvas.drawText("Finance Tracker Android App • Cloud Synced & Biometric Secured • Confidential Financial Report", 32f, 824f, subTitlePaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Financial_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Generates a comprehensive CSV export conforming to standard accounting spreadsheets.
     */
    fun generateCsvExport(
        context: Context,
        transactions: List<TransactionEntity>,
        baseCurrency: String
    ): Uri {
        val file = File(context.cacheDir, "Finance_Ledger_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { writer ->
            // Header
            writer.write("ID,Date,Type,Title,Category,Amount,Currency,AmountInBase($baseCurrency),TaxDeductible,TaxCategory,Notes\n")
            transactions.forEach { tx ->
                val baseAmt = CurrencyManager.convert(tx.amount, tx.currency, baseCurrency)
                val cleanTitle = tx.title.replace("\"", "\"\"")
                val cleanNotes = tx.notes.replace("\"", "\"\"")
                val cleanTaxCat = tx.taxCategory.replace("\"", "\"\"")
                val dateStr = dateFormat.format(Date(tx.date))
                writer.write(
                    "${tx.id},\"$dateStr\",\"${tx.type}\",\"$cleanTitle\",\"${tx.category}\",${tx.amount},\"${tx.currency}\",\"$baseAmt\",${tx.isTaxDeductible},\"$cleanTaxCat\",\"$cleanNotes\"\n"
                )
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Generates a dedicated Tax Compliance CSV export specifically for deductible expenses.
     */
    fun generateTaxComplianceCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        baseCurrency: String
    ): Uri {
        val file = File(context.cacheDir, "Tax_Deduction_Report_${System.currentTimeMillis()}.csv")
        val deductible = transactions.filter { it.isTaxDeductible }
        file.bufferedWriter().use { writer ->
            writer.write("Date,Tax Category,Expense Description,Original Amount,Currency,Converted Amount ($baseCurrency),Notes\n")
            deductible.forEach { tx ->
                val baseAmt = CurrencyManager.convert(tx.amount, tx.currency, baseCurrency)
                val dateStr = dateFormat.format(Date(tx.date))
                val desc = tx.title.replace("\"", "\"\"")
                val notes = tx.notes.replace("\"", "\"\"")
                val taxCat = (if (tx.taxCategory.isNotBlank()) tx.taxCategory else tx.category).replace("\"", "\"\"")
                writer.write(
                    "\"$dateStr\",\"$taxCat\",\"$desc\",${tx.amount},\"${tx.currency}\",\"$baseAmt\",\"$notes\"\n"
                )
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareExportFile(context: Context, uri: Uri, mimeType: String, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is your exported financial report from Finance Tracker.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
