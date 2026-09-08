package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.TransactionEntity
import com.example.export.FinanceExportManager
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TaxBlue
import com.example.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportReportsScreen(
    transactions: List<TransactionEntity>,
    subscriptions: List<SubscriptionEntity>,
    budgets: List<BudgetEntity>,
    baseCurrency: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, MONTH, QUARTER

    val now = System.currentTimeMillis()
    val oneMonthMillis = 30L * 24 * 60 * 60 * 1000L
    val oneQuarterMillis = 90L * 24 * 60 * 60 * 1000L

    val periodTransactions = remember(transactions, selectedPeriod) {
        when (selectedPeriod) {
            "MONTH" -> transactions.filter { it.date >= now - oneMonthMillis }
            "QUARTER" -> transactions.filter { it.date >= now - oneQuarterMillis }
            else -> transactions
        }
    }

    val deductibleTransactions = remember(periodTransactions) {
        periodTransactions.filter { it.isTaxDeductible }
    }

    val totalDeductibleAmount = remember(deductibleTransactions, baseCurrency) {
        deductibleTransactions.sumOf { CurrencyManager.convert(it.amount, it.currency, baseCurrency) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_reports_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Export & Tax Reporting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Export detailed PDF financial statements, CSV sheets, and tax compliance deductions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Period filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPeriod == "ALL",
                    onClick = { selectedPeriod = "ALL" },
                    label = { Text("All Time") }
                )
                FilterChip(
                    selected = selectedPeriod == "MONTH",
                    onClick = { selectedPeriod = "MONTH" },
                    label = { Text("Past 30 Days") }
                )
                FilterChip(
                    selected = selectedPeriod == "QUARTER",
                    onClick = { selectedPeriod = "QUARTER" },
                    label = { Text("Past 90 Days") }
                )
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Eligible Tax Deductions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyManager.formatAmount(totalDeductibleAmount, baseCurrency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaxBlue
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TaxBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${deductibleTransactions.size} Deductible Entries",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaxBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Total Entries in Scope: ${periodTransactions.size} transactions, ${subscriptions.size} subscriptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Export Action Buttons Section
        item {
            Text(
                text = "Generate Reports & Exports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. PDF Report Export Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detailed PDF Financial Statement",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A4 formatted document with summary metrics, tax compliance section, and transaction table.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val uri = FinanceExportManager.generatePdfReport(
                                    context = context,
                                    transactions = periodTransactions,
                                    subscriptions = subscriptions,
                                    budgets = budgets,
                                    baseCurrency = baseCurrency,
                                    periodTitle = if (selectedPeriod == "ALL") "All Time" else "Past $selectedPeriod"
                                )
                                FinanceExportManager.shareExportFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "application/pdf",
                                    title = "Export Financial Statement PDF"
                                )
                                Toast.makeText(context, "PDF Report generated successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "PDF Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_export_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
            }
        }

        // 2. CSV Ledger Export Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Comprehensive CSV Ledger",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standard RFC-4180 CSV export for Excel, Google Sheets, or personal accounting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val uri = FinanceExportManager.generateCsvExport(
                                    context = context,
                                    transactions = periodTransactions,
                                    baseCurrency = baseCurrency
                                )
                                FinanceExportManager.shareExportFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/csv",
                                    title = "Export CSV Ledger"
                                )
                                Toast.makeText(context, "CSV Ledger generated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "CSV Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_export_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
                    }
                }
            }
        }

        // 3. Tax Compliance Deduction Schedule CSV
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TaxBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = TaxBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tax Compliance Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dedicated deductible expense schedule formatted for tax accountant compliance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val uri = FinanceExportManager.generateTaxComplianceCsv(
                                    context = context,
                                    transactions = periodTransactions,
                                    baseCurrency = baseCurrency
                                )
                                FinanceExportManager.shareExportFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/csv",
                                    title = "Export Tax Compliance Schedule"
                                )
                                Toast.makeText(context, "Tax Compliance CSV generated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tax Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TaxBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_export_tax_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tax")
                    }
                }
            }
        }

        // 4. Tax Compliance Deductible Items Preview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Eligible Tax Deductions Log (${deductibleTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (deductibleTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "No tax deductible transactions recorded in this period. Check 'Tax Deductible' when recording business, medical, or charitable expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            items(deductibleTransactions, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tx.taxCategory.ifBlank { "Deductible" }} • ${df.format(Date(tx.date))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TaxBlue
                            )
                            if (tx.notes.isNotBlank()) {
                                Text(
                                    text = "Notes: ${tx.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = CurrencyManager.formatAmount(tx.amount, tx.currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}
