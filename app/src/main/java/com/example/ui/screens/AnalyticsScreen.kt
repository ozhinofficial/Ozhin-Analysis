package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.MonthlyBarData
import com.example.ui.components.MonthlyComparisonBarChart
import com.example.ui.components.SpendingDonutChart
import com.example.ui.dialogs.AddBudgetDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyManager

@Composable
fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    baseCurrency: String,
    onSetBudget: (category: String, limit: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // Prepare Category spending habit breakdown
    val categorySlices = remember(transactions, baseCurrency) {
        val expenseMap = mutableMapOf<String, Double>()
        transactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            val converted = CurrencyManager.convert(tx.amount, tx.currency, baseCurrency)
            expenseMap[tx.category] = (expenseMap[tx.category] ?: 0.0) + converted
        }

        var colorIdx = 0
        expenseMap.entries.sortedByDescending { it.value }.map { entry ->
            val color = com.example.ui.components.CategoryColors[colorIdx % com.example.ui.components.CategoryColors.size]
            colorIdx++
            com.example.ui.components.CategorySlice(category = entry.key, amount = entry.value, color = color)
        }
    }

    // Prepare monthly comparison bar data
    val monthlyBarData = remember(transactions, baseCurrency) {
        val cal = java.util.Calendar.getInstance()
        val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
        val monthKeyFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())

        val result = mutableListOf<MonthlyBarData>()
        for (i in 5 downTo 0) {
            val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -i) }
            val key = monthKeyFormat.format(c.time)
            val label = monthFormat.format(c.time)

            var monthIncome = 0.0
            var monthExpense = 0.0

            transactions.forEach { tx ->
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.date }
                if (monthKeyFormat.format(txCal.time) == key) {
                    val converted = CurrencyManager.convert(tx.amount, tx.currency, baseCurrency)
                    if (tx.type == "INCOME") monthIncome += converted else monthExpense += converted
                }
            }
            result.add(MonthlyBarData(monthLabel = label, income = monthIncome, expense = monthExpense))
        }
        result
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Financial Habits & Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Interactive visual reports and monthly budget discipline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Donut Chart for Monthly Spending Habits
        item {
            SpendingDonutChart(
                slices = categorySlices,
                baseCurrency = baseCurrency
            )
        }

        // 2. Monthly Income vs Expense Bar Chart
        item {
            MonthlyComparisonBarChart(
                data = monthlyBarData,
                baseCurrency = baseCurrency
            )
        }

        // 3. Monthly Budget Analysis Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Budget Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track and control category spending limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddBudgetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_budget")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text("Set Budget")
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No budgets set yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set monthly spending limits for food, rent, shopping, and more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(budgets, key = { it.id }) { budget ->
                // Compute current spent in this category
                val spent = transactions
                    .filter { it.type == "EXPENSE" && it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { CurrencyManager.convert(it.amount, it.currency, baseCurrency) }

                BudgetProgressCard(
                    category = budget.category,
                    spent = spent,
                    limit = budget.monthlyLimit,
                    baseCurrency = baseCurrency,
                    onEditBudget = { showAddBudgetDialog = true }
                )
            }
        }
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            baseCurrency = baseCurrency,
            onDismiss = { showAddBudgetDialog = false },
            onSave = { cat, limit ->
                onSetBudget(cat, limit)
                showAddBudgetDialog = false
            }
        )
    }
}
