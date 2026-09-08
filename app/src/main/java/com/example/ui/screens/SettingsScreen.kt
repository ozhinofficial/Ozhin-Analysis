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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricSecurityManager
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.WarningAmber
import com.example.util.CloudSyncManager
import com.example.util.CurrencyManager
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    lowBalanceThreshold: Double,
    onThresholdChange: (Double) -> Unit,
    lastSyncTime: Long,
    isSyncing: Boolean,
    syncMessage: String?,
    onClearSyncMessage: () -> Unit,
    onSyncWithCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onTriggerTestNotification: () -> Unit,
    authManager: com.example.auth.AuthManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var baseCurrencyExpanded by remember { mutableStateOf(false) }
    val currentUser by (authManager?.currentUser ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState(initial = null)
    var showAuthDialog by remember { mutableStateOf(false) }

    // Multi-Currency Live Converter Tool State
    var converterAmount by remember { mutableStateOf("100") }
    var converterFrom by remember { mutableStateOf("USD") }
    var converterTo by remember { mutableStateOf("EUR") }
    var converterFromExpanded by remember { mutableStateOf(false) }
    var converterToExpanded by remember { mutableStateOf(false) }

    // PIN Setup Dialog
    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf(lowBalanceThreshold.toString()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & Security",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure cloud synchronization, biometric privacy, currencies, and notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 0. User Authentication & Cloud Account Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("auth_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentUser != null) "Logged In Account" else "User Authentication",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser != null) {
                                        currentUser?.email ?: if (currentUser?.isAnonymous == true) "Guest Cloud Account" else "Active User"
                                    } else {
                                        "Sign in to secure financial data across devices"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Account UID: ${currentUser?.uid?.take(12)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Authentication: ${if (currentUser?.isAnonymous == true) "Anonymous / Guest" else "Email / Password"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { authManager?.signOut() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_auth_signout")
                        ) {
                            Text("Sign Out")
                        }
                    } else {
                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_login")
                        ) {
                            Text("Sign In or Register")
                        }
                    }
                }
            }
        }

        // 1. Cloud Synchronization Across Devices
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("cloud_sync_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0EA5E9).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF0EA5E9))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Synchronization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Seamless cross-device encrypted sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Device Node:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CloudSyncManager.getDeviceId(context),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Last Cloud Sync:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CloudSyncManager.formatLastSync(lastSyncTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0EA5E9)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncWithCloud,
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_sync_cloud_now")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }

                        OutlinedButton(
                            onClick = onRestoreFromCloud,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_restore_cloud")
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }

                    if (syncMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Biometric Security & PIN Protection
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("security_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric & App Security",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Protect financial records with fingerprint / PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Biometric Authentication", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { onToggleBiometric(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("switch_biometric")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons for PIN and Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_configure_pin")
                        ) {
                            Text("Set 4-Digit PIN")
                        }

                        Button(
                            onClick = onLockAppNow,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("btn_lock_now")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock App")
                        }
                    }
                }
            }
        }

        // 3. Multi-Currency Global Tracking & Live Converter
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("currency_converter_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = WarningAmber)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Multi-Currency Global Tracking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base currency & real-time conversion rates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Base Currency Selector
                    ExposedDropdownMenuBox(
                        expanded = baseCurrencyExpanded,
                        onExpandedChange = { baseCurrencyExpanded = !baseCurrencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentItem = CurrencyManager.getCurrency(baseCurrency)
                        OutlinedTextField(
                            value = "${currentItem.flag} ${currentItem.name} (${currentItem.code} ${currentItem.symbol})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary Base Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseCurrencyExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_base_currency")
                        )
                        ExposedDropdownMenu(
                            expanded = baseCurrencyExpanded,
                            onDismissRequest = { baseCurrencyExpanded = false }
                        ) {
                            CurrencyManager.supportedCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.flag} ${curr.name} (${curr.code} ${curr.symbol})") },
                                    onClick = {
                                        onBaseCurrencyChange(curr.code)
                                        baseCurrencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Currency Converter Tool
                    Text(
                        text = "Instant Multi-Currency Converter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = converterAmount,
                            onValueChange = { converterAmount = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("converter_amount_input")
                        )

                        // From Dropdown
                        ExposedDropdownMenuBox(
                            expanded = converterFromExpanded,
                            onExpandedChange = { converterFromExpanded = !converterFromExpanded },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            OutlinedTextField(
                                value = converterFrom,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("From") },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = converterFromExpanded,
                                onDismissRequest = { converterFromExpanded = false }
                            ) {
                                CurrencyManager.supportedCurrencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.flag} ${c.code}") },
                                        onClick = {
                                            converterFrom = c.code
                                            converterFromExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

                        // To Dropdown
                        ExposedDropdownMenuBox(
                            expanded = converterToExpanded,
                            onExpandedChange = { converterToExpanded = !converterToExpanded },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            OutlinedTextField(
                                value = converterTo,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("To") },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = converterToExpanded,
                                onDismissRequest = { converterToExpanded = false }
                            ) {
                                CurrencyManager.supportedCurrencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.flag} ${c.code}") },
                                        onClick = {
                                            converterTo = c.code
                                            converterToExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation Result Box
                    val amtVal = converterAmount.toDoubleOrNull() ?: 0.0
                    val convertedVal = CurrencyManager.convert(amtVal, converterFrom, converterTo)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Converted Amount:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyManager.formatAmount(convertedVal, converterTo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyManager.getRateString(converterFrom, converterTo),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Notifications & Account Balance Warnings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = WarningAmber)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Balance Warnings & Bill Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Threshold triggers and bill due push alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = thresholdInput,
                            onValueChange = {
                                thresholdInput = it
                                it.toDoubleOrNull()?.let { num -> onThresholdChange(num) }
                            },
                            label = { Text("Low Balance Warning Limit ($baseCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_balance_threshold")
                        )

                        Button(
                            onClick = onTriggerTestNotification,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_test_notification")
                        ) {
                            Text("Test Alert", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Dark Mode & Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Mode Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDarkMode) "Dark slate canvas active" else "Light clean canvas active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("switch_dark_mode")
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a 4-digit numeric passcode to protect your records when biometrics are unavailable.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_set_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            BiometricSecurityManager.setPin(context, newPinInput)
                            Toast.makeText(context, "PIN code saved!", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                        } else {
                            Toast.makeText(context, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAuthDialog && authManager != null) {
        com.example.ui.dialogs.AuthDialog(
            authManager = authManager,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = { showAuthDialog = false }
        )
    }
}

