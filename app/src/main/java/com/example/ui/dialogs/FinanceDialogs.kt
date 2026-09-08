package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.camera.ReceiptCameraDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyManager
import java.io.File

val DefaultCategories = listOf(
    "Food & Dining",
    "Housing",
    "Transportation",
    "Utilities",
    "Entertainment",
    "Healthcare",
    "Shopping",
    "Salary",
    "Freelance",
    "Investment",
    "Education",
    "Travel",
    "Personal Care",
    "Other"
)

val TaxCategories = listOf(
    "Home Office Expense",
    "501(c)(3) Charitable Contribution",
    "Medical & Dental",
    "Business Travel & Mileage",
    "Continuing Education & Certifications",
    "Software & Professional Tools",
    "General Deductible"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        notes: String,
        currency: String,
        isTaxDeductible: Boolean,
        taxCategory: String,
        receiptImagePath: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Food & Dining") }
    var notes by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(baseCurrency) }
    var isTaxDeductible by remember { mutableStateOf(false) }
    var taxCategory by remember { mutableStateOf("Home Office Expense") }
    var receiptImagePath by remember { mutableStateOf("") }
    var showCameraDialog by remember { mutableStateOf(false) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var taxCatExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_transaction_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Record Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Selector: Expense vs Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                            if (category == "Salary" || category == "Freelance") {
                                category = "Food & Dining"
                            }
                        },
                        label = { Text("Expense", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("chip_type_expense"),
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseRed
                        )
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                            category = "Salary"
                        },
                        label = { Text("Income", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("chip_type_income"),
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                            selectedLabelColor = IncomeGreen
                        )
                    )
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / Title *") },
                    placeholder = { Text("e.g. Grocery Store, Client Invoice") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_title")
                )

                // Amount & Currency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount *") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f).testTag("input_tx_amount")
                    )

                    // Currency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            CurrencyManager.supportedCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.flag} ${curr.code} (${curr.symbol})") },
                                    onClick = {
                                        selectedCurrency = curr.code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DefaultCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add memo, invoice #, receipt details...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_notes")
                )

                // Tax Deductible Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTaxDeductible = !isTaxDeductible }
                        ) {
                            Checkbox(
                                checked = isTaxDeductible,
                                onCheckedChange = { isTaxDeductible = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Tax Deductible Expense",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Include in tax compliance export & report",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isTaxDeductible) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = taxCatExpanded,
                                onExpandedChange = { taxCatExpanded = !taxCatExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = taxCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tax Category Schedule") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxCatExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = taxCatExpanded,
                                    onDismissRequest = { taxCatExpanded = false }
                                ) {
                                    TaxCategories.forEach { tc ->
                                        DropdownMenuItem(
                                            text = { Text(tc) },
                                            onClick = {
                                                taxCategory = tc
                                                taxCatExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Receipt Photo Capture Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().testTag("receipt_photo_section")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Receipt Photo Proof",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (receiptImagePath.isNotEmpty()) "Receipt photo attached" else "Take photo for tax & expense records",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (receiptImagePath.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = File(receiptImagePath),
                                    contentDescription = "Receipt Snapshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { receiptImagePath = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove receipt",
                                        tint = ExpenseRed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showCameraDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("btn_retake_receipt")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake Receipt Photo")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showCameraDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("btn_snap_receipt")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Receipt Photo with Camera")
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Please enter a transaction title"
                    } else if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                    } else {
                        onSave(
                            title,
                            amt,
                            type,
                            category,
                            notes,
                            selectedCurrency,
                            isTaxDeductible,
                            if (isTaxDeductible) taxCategory else "",
                            receiptImagePath
                        )
                    }
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCameraDialog) {
        ReceiptCameraDialog(
            onDismiss = { showCameraDialog = false },
            onReceiptCaptured = { path ->
                receiptImagePath = path
                showCameraDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        amount: Double,
        currency: String,
        billingCycle: String,
        nextDueDate: Long,
        category: String,
        notes: String,
        reminderDaysBefore: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var billingCycle by remember { mutableStateOf("MONTHLY") }
    var category by remember { mutableStateOf("Subscriptions") }
    var notes by remember { mutableStateOf("") }
    var reminderDaysBefore by remember { mutableStateOf(3) }
    var daysUntilDueInput by remember { mutableStateOf("7") }

    var currencyExpanded by remember { mutableStateOf(false) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cycles = listOf("MONTHLY", "YEARLY", "WEEKLY")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add Recurring Subscription",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subscription Name *") },
                    placeholder = { Text("e.g. Netflix, Gym, AWS Cloud") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_sub_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount *") },
                        placeholder = { Text("14.99") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f).testTag("input_sub_amount")
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            CurrencyManager.supportedCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.flag} ${curr.code}") },
                                    onClick = {
                                        currency = curr.code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Billing Cycle
                ExposedDropdownMenuBox(
                    expanded = cycleExpanded,
                    onExpandedChange = { cycleExpanded = !cycleExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = billingCycle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Billing Cycle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = cycleExpanded,
                        onDismissRequest = { cycleExpanded = false }
                    ) {
                        cycles.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    billingCycle = c
                                    cycleExpanded = false
                                }
                            )
                        }
                    }
                }

                // Days until next due date
                OutlinedTextField(
                    value = daysUntilDueInput,
                    onValueChange = { daysUntilDueInput = it },
                    label = { Text("Due in (Days from now) *") },
                    placeholder = { Text("e.g. 3, 7, 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_sub_due_days")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Cancellation Memo") },
                    placeholder = { Text("Account #, renewal terms, cancel url...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val days = daysUntilDueInput.toLongOrNull()
                    if (name.isBlank()) {
                        errorMessage = "Please enter subscription name"
                    } else if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount"
                    } else if (days == null || days < 0) {
                        errorMessage = "Please enter valid days until due date"
                    } else {
                        val nextDueDate = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                        onSave(
                            name,
                            amt,
                            currency,
                            billingCycle,
                            nextDueDate,
                            category,
                            notes,
                            reminderDaysBefore
                        )
                    }
                },
                modifier = Modifier.testTag("save_subscription_button")
            ) {
                Text("Track Subscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double) -> Unit
) {
    var category by remember { mutableStateOf("Food & Dining") }
    var limitText by remember { mutableStateOf("500.00") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Set Monthly Category Budget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DefaultCategories.filter { it != "Salary" && it != "Freelance" && it != "Investment" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Spending Limit ($baseCurrency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_budget_limit")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        errorMessage = "Please enter a valid budget limit"
                    } else {
                        onSave(category, limit)
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
