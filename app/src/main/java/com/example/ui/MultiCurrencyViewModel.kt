package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.ExchangeRateRepository
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyItem
import com.example.util.CurrencyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class ConvertedCurrencyTotal(
    val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val flag: String,
    val rateAgainstBase: Double,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netBalance: Double,
    val formattedIncome: String,
    val formattedExpenses: String,
    val formattedBalance: String,
    val convertedCustomAmount: Double,
    val formattedCustomAmount: String
)

data class MultiCurrencyUiState(
    val baseCurrency: String = "USD",
    val baseIncome: Double = 0.0,
    val baseExpenses: Double = 0.0,
    val baseNetBalance: Double = 0.0,
    val transactionCount: Int = 0,
    val customAmountInput: String = "100",
    val selectedTargetCurrencies: Set<String> = CurrencyManager.supportedCurrencies.map { it.code }.toSet(),
    val convertedTotals: List<ConvertedCurrencyTotal> = emptyList(),
    val availableCurrencies: List<CurrencyItem> = CurrencyManager.supportedCurrencies,
    val isLoading: Boolean = false,
    val isLiveApi: Boolean = false,
    val lastUpdated: String = "",
    val errorMessage: String? = null
)

class MultiCurrencyViewModel(
    application: Application,
    private val financeRepository: FinanceRepository = FinanceRepository(
        AppDatabase.getDatabase(application).transactionDao(),
        AppDatabase.getDatabase(application).subscriptionDao(),
        AppDatabase.getDatabase(application).budgetDao()
    ),
    private val userPreferences: UserPreferencesManager = UserPreferencesManager(application),
    private val exchangeRateRepository: ExchangeRateRepository = ExchangeRateRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MultiCurrencyUiState())
    val uiState: StateFlow<MultiCurrencyUiState> = _uiState.asStateFlow()

    private val _ratesMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    private val _selectedCurrencies = MutableStateFlow(
        CurrencyManager.supportedCurrencies.map { it.code }.toSet()
    )
    private val _customAmount = MutableStateFlow("100")
    private val _isLoading = MutableStateFlow(false)
    private val _isLiveApi = MutableStateFlow(false)
    private val _lastUpdated = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        // Observe base currency from preferences and observe transactions from repository
        viewModelScope.launch {
            combine(
                userPreferences.baseCurrencyFlow,
                financeRepository.allTransactions,
                _ratesMap,
                _selectedCurrencies,
                _customAmount,
                _isLoading,
                _isLiveApi,
                _lastUpdated,
                _errorMessage
            ) { values ->
                val baseCurr = values[0] as String
                @Suppress("UNCHECKED_CAST")
                val transactionsList = values[1] as List<TransactionEntity>
                @Suppress("UNCHECKED_CAST")
                val rates = values[2] as Map<String, Double>
                @Suppress("UNCHECKED_CAST")
                val selectedCodes = values[3] as Set<String>
                val customAmtStr = values[4] as String
                val loading = values[5] as Boolean
                val live = values[6] as Boolean
                val updated = values[7] as String
                val error = values[8] as String?

                buildUiState(
                    baseCurrency = baseCurr,
                    transactions = transactionsList,
                    rates = rates,
                    selectedCurrencies = selectedCodes,
                    customAmountStr = customAmtStr,
                    isLoading = loading,
                    isLiveApi = live,
                    lastUpdated = updated,
                    errorMessage = error
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }

        // Fetch initial exchange rates
        fetchRates(forceRefresh = false)
    }

    /**
     * Fetches real-time exchange rates from the API.
     */
    fun fetchRates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val currentBase = _uiState.value.baseCurrency.ifBlank { "USD" }
                val rateData = exchangeRateRepository.getExchangeRates(currentBase, forceRefresh)
                _ratesMap.value = rateData.rates
                _isLiveApi.value = rateData.isLive
                _lastUpdated.value = rateData.lastUpdatedUtc
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update live rates: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the base currency and re-fetches exchange rates.
     */
    fun setBaseCurrency(newBase: String) {
        viewModelScope.launch {
            userPreferences.setBaseCurrency(newBase)
            fetchRates(forceRefresh = true)
        }
    }

    /**
     * Toggles a target currency in the multi-currency conversion list.
     */
    fun toggleTargetCurrency(currencyCode: String) {
        val current = _selectedCurrencies.value.toMutableSet()
        if (current.contains(currencyCode)) {
            // Keep at least one currency selected
            if (current.size > 1) {
                current.remove(currencyCode)
            }
        } else {
            current.add(currencyCode)
        }
        _selectedCurrencies.value = current
        _uiState.value = _uiState.value.copy(
            selectedTargetCurrencies = current,
            convertedTotals = _uiState.value.convertedTotals.filter { current.contains(it.currencyCode) }
        )
    }

    fun selectAllCurrencies() {
        val all = CurrencyManager.supportedCurrencies.map { it.code }.toSet()
        _selectedCurrencies.value = all
        _uiState.value = _uiState.value.copy(selectedTargetCurrencies = all)
    }

    fun deselectAllCurrencies() {
        // Keep base currency selected
        val baseOnly = setOf(_uiState.value.baseCurrency)
        _selectedCurrencies.value = baseOnly
        _uiState.value = _uiState.value.copy(
            selectedTargetCurrencies = baseOnly,
            convertedTotals = _uiState.value.convertedTotals.filter { baseOnly.contains(it.currencyCode) }
        )
    }

    fun setCustomAmount(amount: String) {
        _customAmount.value = amount
        val customAmt = amount.toDoubleOrNull() ?: 0.0
        val updatedConverted = _uiState.value.convertedTotals.map { item ->
            val converted = customAmt * item.rateAgainstBase
            item.copy(
                convertedCustomAmount = converted,
                formattedCustomAmount = formatAmount(converted, item.currencyCode, item.symbol)
            )
        }
        _uiState.value = _uiState.value.copy(
            customAmountInput = amount,
            convertedTotals = updatedConverted
        )
    }

    /**
     * Utility to convert an arbitrary amount between two currencies using current rates.
     */
    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        return exchangeRateRepository.convert(amount, fromCurrency, toCurrency, _ratesMap.value)
    }

    private fun buildUiState(
        baseCurrency: String,
        transactions: List<TransactionEntity>,
        rates: Map<String, Double>,
        selectedCurrencies: Set<String>,
        customAmountStr: String,
        isLoading: Boolean,
        isLiveApi: Boolean,
        lastUpdated: String,
        errorMessage: String?
    ): MultiCurrencyUiState {
        // 1. Calculate Base Totals (converting each transaction to baseCurrency)
        var totalIncomeBase = 0.0
        var totalExpensesBase = 0.0

        transactions.forEach { tx ->
            val convertedTxAmount = exchangeRateRepository.convert(
                amount = tx.amount,
                fromCurrency = tx.currency,
                toCurrency = baseCurrency,
                ratesMap = rates
            )
            if (tx.type.equals("INCOME", ignoreCase = true)) {
                totalIncomeBase += convertedTxAmount
            } else {
                totalExpensesBase += convertedTxAmount
            }
        }
        val netBalanceBase = totalIncomeBase - totalExpensesBase

        val customAmt = customAmountStr.toDoubleOrNull() ?: 0.0

        // 2. Convert base totals across all selected target currencies
        val convertedList = CurrencyManager.supportedCurrencies
            .filter { selectedCurrencies.contains(it.code) }
            .map { targetItem ->
                val targetCode = targetItem.code
                // Rate from baseCurrency to targetCode
                val rate = when {
                    targetCode.equals(baseCurrency, ignoreCase = true) -> 1.0
                    rates.containsKey(targetCode) -> rates.getValue(targetCode)
                    else -> exchangeRateRepository.convert(1.0, baseCurrency, targetCode, rates)
                }

                val convertedIncome = totalIncomeBase * rate
                val convertedExpenses = totalExpensesBase * rate
                val convertedBalance = netBalanceBase * rate
                val convertedCustom = customAmt * rate

                ConvertedCurrencyTotal(
                    currencyCode = targetCode,
                    currencyName = targetItem.name,
                    symbol = targetItem.symbol,
                    flag = targetItem.flag,
                    rateAgainstBase = rate,
                    totalIncome = convertedIncome,
                    totalExpenses = convertedExpenses,
                    netBalance = convertedBalance,
                    formattedIncome = formatAmount(convertedIncome, targetCode, targetItem.symbol),
                    formattedExpenses = formatAmount(convertedExpenses, targetCode, targetItem.symbol),
                    formattedBalance = formatAmount(convertedBalance, targetCode, targetItem.symbol),
                    convertedCustomAmount = convertedCustom,
                    formattedCustomAmount = formatAmount(convertedCustom, targetCode, targetItem.symbol)
                )
            }

        return MultiCurrencyUiState(
            baseCurrency = baseCurrency,
            baseIncome = totalIncomeBase,
            baseExpenses = totalExpensesBase,
            baseNetBalance = netBalanceBase,
            transactionCount = transactions.size,
            customAmountInput = customAmountStr,
            selectedTargetCurrencies = selectedCurrencies,
            convertedTotals = convertedList,
            availableCurrencies = CurrencyManager.supportedCurrencies,
            isLoading = isLoading,
            isLiveApi = isLiveApi,
            lastUpdated = lastUpdated,
            errorMessage = errorMessage
        )
    }

    private fun formatAmount(amount: Double, currencyCode: String, symbol: String): String {
        val pattern = if (currencyCode.equals("JPY", ignoreCase = true)) "#,##0" else "#,##0.00"
        val formattedNumber = DecimalFormat(pattern).format(amount)
        return "$symbol$formattedNumber"
    }
}
