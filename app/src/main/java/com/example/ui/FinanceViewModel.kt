package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.SubscriptionEntity
import com.example.data.model.TransactionEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.FinanceRepository
import com.example.notification.FinanceNotificationManager
import com.example.security.BiometricSecurityManager
import com.example.ui.components.CategoryColors
import com.example.ui.components.CategorySlice
import com.example.ui.components.MonthlyBarData
import com.example.util.CloudSyncManager
import com.example.util.CurrencyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = com.example.auth.AuthManager(application)
    val currentUser = authManager.currentUser

    private val repository: FinanceRepository
    private val notificationManager = FinanceNotificationManager(application)
    private val userPreferences = UserPreferencesManager(application)

    val transactions: StateFlow<List<TransactionEntity>>
    val subscriptions: StateFlow<List<SubscriptionEntity>>
    val budgets: StateFlow<List<BudgetEntity>>

    val baseCurrency: StateFlow<String> = userPreferences.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "USD")

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    val lowBalanceThreshold: StateFlow<Double> = userPreferences.lowBalanceThresholdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 500.0)

    val isDarkMode: StateFlow<Boolean> = userPreferences.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db.transactionDao(), db.subscriptionDao(), db.budgetDao())

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        subscriptions = repository.allSubscriptions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        budgets = repository.allBudgets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Check if biometric/security was previously enabled
        val securityEnabled = BiometricSecurityManager.isSecurityEnabled(application)
        _isBiometricEnabled.value = BiometricSecurityManager.isBiometricEnabled(application)
        _isLocked.value = securityEnabled

        _lastSyncTimestamp.value = CloudSyncManager.getLastSyncTimestamp(application)

        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockApp() {
        _isLocked.value = true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        BiometricSecurityManager.setBiometricEnabled(getApplication(), enabled)
    }

    fun setBaseCurrency(currencyCode: String) {
        viewModelScope.launch {
            userPreferences.setBaseCurrency(currencyCode)
        }
    }

    fun setLowBalanceThreshold(threshold: Double) {
        viewModelScope.launch {
            userPreferences.setLowBalanceThreshold(threshold)
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            userPreferences.setDarkMode(!isDarkMode.value)
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(dark)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Transaction actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        notes: String,
        currency: String,
        isTaxDeductible: Boolean,
        taxCategory: String,
        receiptImagePath: String = ""
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category.trim(),
                notes = notes.trim(),
                date = System.currentTimeMillis(),
                currency = currency,
                isTaxDeductible = isTaxDeductible,
                taxCategory = taxCategory.trim(),
                receiptImagePath = receiptImagePath
            )
            repository.insertTransaction(tx)

            // Trigger notification checks after transaction
            checkBalanceAndUpcomingNotifications()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            checkBalanceAndUpcomingNotifications()
        }
    }

    // Subscription actions
    fun addSubscription(
        name: String,
        amount: Double,
        currency: String,
        billingCycle: String,
        nextDueDate: Long,
        category: String,
        notes: String,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            val sub = SubscriptionEntity(
                name = name.trim(),
                amount = amount,
                currency = currency,
                billingCycle = billingCycle,
                nextDueDate = nextDueDate,
                category = category.trim(),
                notes = notes.trim(),
                reminderDaysBefore = reminderDaysBefore
            )
            repository.insertSubscription(sub)
            checkBalanceAndUpcomingNotifications()
        }
    }

    fun markSubscriptionPaid(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            // 1. Record an expense transaction for the payment
            val tx = TransactionEntity(
                title = "${subscription.name} (Recurring Payment)",
                amount = subscription.amount,
                type = "EXPENSE",
                category = subscription.category,
                notes = "Auto-logged subscription renewal payment for ${subscription.billingCycle.lowercase()} cycle.",
                date = System.currentTimeMillis(),
                currency = subscription.currency,
                isTaxDeductible = false
            )
            repository.insertTransaction(tx)

            // 2. Advance next due date according to billing cycle
            val cal = Calendar.getInstance().apply { timeInMillis = subscription.nextDueDate }
            when (subscription.billingCycle) {
                "WEEKLY" -> cal.add(Calendar.DAY_OF_YEAR, 7)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1) // MONTHLY
            }

            val updatedSub = subscription.copy(nextDueDate = cal.timeInMillis)
            repository.updateSubscription(updatedSub)

            checkBalanceAndUpcomingNotifications()
        }
    }

    fun toggleSubscriptionActive(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.updateSubscription(subscription.copy(isActive = !subscription.isActive))
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }

    // Budget actions
    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val budget = BudgetEntity(
                category = category,
                monthlyLimit = limit,
                monthYear = currentMonth,
                currency = baseCurrency.value
            )
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Cloud Synchronization
    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = CloudSyncManager.syncWithCloud(
                    getApplication(),
                    transactions.value,
                    subscriptions.value,
                    budgets.value
                )
                _lastSyncTimestamp.value = result.timestamp
                _syncMessage.value = result.message
            } catch (e: Exception) {
                _syncMessage.value = "Cloud sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val payload = CloudSyncManager.getCloudBackupPayload(getApplication())
                if (payload != null) {
                    repository.restoreAll(
                        payload.transactions,
                        payload.subscriptions,
                        payload.budgets
                    )
                    _lastSyncTimestamp.value = payload.timestamp
                    _syncMessage.value = "Successfully restored ${payload.transactions.size} records from Cloud backup."
                } else {
                    _syncMessage.value = "No existing cloud backup found on this account."
                }
            } catch (e: Exception) {
                _syncMessage.value = "Cloud restore failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Real-time notification checks
    fun checkBalanceAndUpcomingNotifications() {
        val curr = baseCurrency.value
        val txList = transactions.value

        var totalInc = 0.0
        var totalExp = 0.0
        txList.forEach {
            val converted = CurrencyManager.convert(it.amount, it.currency, curr)
            if (it.type == "INCOME") totalInc += converted else totalExp += converted
        }
        val netBalance = totalInc - totalExp

        // 1. Account balance threshold warning
        notificationManager.checkBalance(netBalance, lowBalanceThreshold.value, curr)

        // 2. Upcoming bills check
        notificationManager.checkAndAlertBills(subscriptions.value, curr)
    }

    fun triggerTestNotification() {
        notificationManager.notifyBalanceWarning(
            CurrencyManager.formatAmount(240.0, baseCurrency.value),
            CurrencyManager.formatAmount(lowBalanceThreshold.value, baseCurrency.value)
        )
    }

    // Helper computation functions
    fun calculateCategorySlices(txList: List<TransactionEntity>, baseCurr: String): List<CategorySlice> {
        val expenseMap = mutableMapOf<String, Double>()
        txList.filter { it.type == "EXPENSE" }.forEach { tx ->
            val converted = CurrencyManager.convert(tx.amount, tx.currency, baseCurr)
            expenseMap[tx.category] = (expenseMap[tx.category] ?: 0.0) + converted
        }

        var colorIdx = 0
        return expenseMap.entries.sortedByDescending { it.value }.map { entry ->
            val color = CategoryColors[colorIdx % CategoryColors.size]
            colorIdx++
            CategorySlice(category = entry.key, amount = entry.value, color = color)
        }
    }

    fun calculateMonthlyBarData(txList: List<TransactionEntity>, baseCurr: String): List<MonthlyBarData> {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val result = mutableListOf<MonthlyBarData>()
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
            }
            val key = monthKeyFormat.format(c.time)
            val label = monthFormat.format(c.time)

            var monthIncome = 0.0
            var monthExpense = 0.0

            txList.forEach { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                if (monthKeyFormat.format(txCal.time) == key) {
                    val converted = CurrencyManager.convert(tx.amount, tx.currency, baseCurr)
                    if (tx.type == "INCOME") monthIncome += converted else monthExpense += converted
                }
            }
            result.add(MonthlyBarData(monthLabel = label, income = monthIncome, expense = monthExpense))
        }
        return result
    }
}
