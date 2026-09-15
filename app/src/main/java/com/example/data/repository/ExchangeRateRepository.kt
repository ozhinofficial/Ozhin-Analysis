package com.example.data.repository

import android.util.Log
import com.example.data.network.ExchangeRateApiService
import com.example.data.network.NetworkClient
import com.example.util.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExchangeRatesData(
    val baseCode: String,
    val rates: Map<String, Double>,
    val lastUpdatedUtc: String,
    val isLive: Boolean
)

class ExchangeRateRepository(
    private val apiService: ExchangeRateApiService = NetworkClient.exchangeRateApi
) {
    private val cache = mutableMapOf<String, ExchangeRatesData>()

    /**
     * Fetches exchange rates for the given base currency.
     * Uses cache if fresh, otherwise calls the live API with a robust fallback to static rates.
     */
    suspend fun getExchangeRates(baseCurrency: String, forceRefresh: Boolean = false): ExchangeRatesData =
        withContext(Dispatchers.IO) {
            val upperBase = baseCurrency.uppercase()

            if (!forceRefresh && cache.containsKey(upperBase)) {
                return@withContext cache.getValue(upperBase)
            }

            try {
                val response = apiService.getLatestRates(upperBase)
                if (response.result.equals("success", ignoreCase = true) && response.rates.isNotEmpty()) {
                    val data = ExchangeRatesData(
                        baseCode = response.baseCode,
                        rates = response.rates,
                        lastUpdatedUtc = response.timeLastUpdateUtc ?: "Recent",
                        isLive = true
                    )
                    cache[upperBase] = data
                    return@withContext data
                }
            } catch (e: Exception) {
                Log.w("ExchangeRateRepository", "Live exchange rate fetch failed for $upperBase: ${e.message}")
            }

            // Fallback to local offline rates from CurrencyManager
            val fallbackRates = mutableMapOf<String, Double>()
            val baseItem = CurrencyManager.getCurrency(upperBase)
            CurrencyManager.supportedCurrencies.forEach { item ->
                // baseItem.rateToUsd is USD->base, item.rateToUsd is USD->item
                // 1 base = (1 / baseItem.rateToUsd) * item.rateToUsd
                val rate = (1.0 / baseItem.rateToUsd) * item.rateToUsd
                fallbackRates[item.code] = rate
            }

            val fallbackData = ExchangeRatesData(
                baseCode = upperBase,
                rates = fallbackRates,
                lastUpdatedUtc = "Offline Standard Rates",
                isLive = false
            )
            cache[upperBase] = fallbackData
            fallbackData
        }

    /**
     * Converts an amount from one currency to another using the cached or fallback rates.
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        ratesMap: Map<String, Double>? = null
    ): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount

        val fromUpper = fromCurrency.uppercase()
        val toUpper = toCurrency.uppercase()

        if (ratesMap != null) {
            val fromRate = ratesMap[fromUpper]
            val toRate = ratesMap[toUpper]
            if (fromRate != null && toRate != null && fromRate > 0.0) {
                // If base is B: 1 B = fromRate of From, 1 B = toRate of To
                // Amount in B = amount / fromRate
                // Amount in To = (amount / fromRate) * toRate
                return (amount / fromRate) * toRate
            }
        }

        // Fallback to CurrencyManager
        return CurrencyManager.convert(amount, fromUpper, toUpper)
    }
}
