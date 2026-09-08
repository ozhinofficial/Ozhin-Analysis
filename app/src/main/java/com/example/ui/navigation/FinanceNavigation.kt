package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceViewModel
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.security.BiometricLockOverlay
import com.example.ui.theme.EmeraldPrimary

enum class FinanceTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.AccountBalanceWallet, "tab_dashboard"),
    ANALYTICS("Analytics", Icons.Default.PieChart, "tab_analytics"),
    SUBSCRIPTIONS("Bills", Icons.Default.Repeat, "tab_subscriptions"),
    REPORTS("Reports", Icons.Default.Description, "tab_reports"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(
    viewModel: FinanceViewModel = viewModel()
) {
    val isLocked by viewModel.isLocked.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val lowBalanceThreshold by viewModel.lowBalanceThreshold.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var currentTab by remember { mutableStateOf(FinanceTab.DASHBOARD) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    if (isLocked) {
        BiometricLockOverlay(
            onUnlock = { viewModel.unlockApp() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                FinanceTab.DASHBOARD -> "Finance Tracker"
                                FinanceTab.ANALYTICS -> "Spending Analytics"
                                FinanceTab.SUBSCRIPTIONS -> "Subscription Tracker"
                                FinanceTab.REPORTS -> "Export & Tax Reporting"
                                FinanceTab.SETTINGS -> "Preferences"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("action_toggle_dark")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark Mode"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.lockApp() },
                            modifier = Modifier.testTag("action_lock_app")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock App"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    FinanceTab.values().forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            modifier = Modifier.testTag(tab.tag),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    FinanceTab.DASHBOARD -> {
                        DashboardScreen(
                            transactions = transactions,
                            subscriptions = subscriptions,
                            baseCurrency = baseCurrency,
                            lowBalanceThreshold = lowBalanceThreshold,
                            onAddTransactionClick = { showAddTransactionDialog = true },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            onNavigateToSubscriptions = { currentTab = FinanceTab.SUBSCRIPTIONS }
                        )
                    }
                    FinanceTab.ANALYTICS -> {
                        AnalyticsScreen(
                            transactions = transactions,
                            budgets = budgets,
                            baseCurrency = baseCurrency,
                            onSetBudget = { cat, limit -> viewModel.setBudget(cat, limit) }
                        )
                    }
                    FinanceTab.SUBSCRIPTIONS -> {
                        SubscriptionsScreen(
                            subscriptions = subscriptions,
                            baseCurrency = baseCurrency,
                            onAddSubscription = { name, amt, curr, cycle, nextDate, cat, notes, remDays ->
                                viewModel.addSubscription(name, amt, curr, cycle, nextDate, cat, notes, remDays)
                            },
                            onMarkPaid = { viewModel.markSubscriptionPaid(it) },
                            onToggleActive = { viewModel.toggleSubscriptionActive(it) },
                            onDeleteSubscription = { viewModel.deleteSubscription(it) }
                        )
                    }
                    FinanceTab.REPORTS -> {
                        ExportReportsScreen(
                            transactions = transactions,
                            subscriptions = subscriptions,
                            budgets = budgets,
                            baseCurrency = baseCurrency
                        )
                    }
                    FinanceTab.SETTINGS -> {
                        SettingsScreen(
                            baseCurrency = baseCurrency,
                            onBaseCurrencyChange = { viewModel.setBaseCurrency(it) },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            isBiometricEnabled = isBiometricEnabled,
                            onToggleBiometric = { viewModel.setBiometricEnabled(it) },
                            onLockAppNow = { viewModel.lockApp() },
                            lowBalanceThreshold = lowBalanceThreshold,
                            onThresholdChange = { viewModel.setLowBalanceThreshold(it) },
                            lastSyncTime = lastSyncTimestamp,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            onClearSyncMessage = { viewModel.clearSyncMessage() },
                            onSyncWithCloud = { viewModel.syncWithCloud() },
                            onRestoreFromCloud = { viewModel.restoreFromCloud() },
                            onTriggerTestNotification = { viewModel.triggerTestNotification() },
                            authManager = viewModel.authManager
                        )
                    }
                }
            }
        }
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            baseCurrency = baseCurrency,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { title, amt, type, cat, notes, curr, isDeductible, taxCat, receiptPath ->
                viewModel.addTransaction(title, amt, type, cat, notes, curr, isDeductible, taxCat, receiptPath)
                showAddTransactionDialog = false
            }
        )
    }
}
