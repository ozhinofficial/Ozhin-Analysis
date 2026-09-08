package com.example.util

import java.text.DecimalFormat
import java.util.Locale

data class CurrencyItem(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateToUsd: Double // 1 USD = rateToUsd in this currency
)

object CurrencyManager {
    val supportedCurrencies = listOf(
        CurrencyItem("USD", "US Dollar", "$", "🇺🇸", 1.0),
        CurrencyItem("EUR", "Euro", "€", "🇪🇺", 0.92),
        CurrencyItem("GBP", "British Pound", "£", "🇬🇧", 0.78),
        CurrencyItem("JPY", "Japanese Yen", "¥", "🇯🇵", 154.50),
        CurrencyItem("CAD", "Canadian Dollar", "C$", "🇨🇦", 1.36),
        CurrencyItem("AUD", "Australian Dollar", "A$", "🇦🇺", 1.51),
        CurrencyItem("INR", "Indian Rupee", "₹", "🇮🇳", 83.50),
        CurrencyItem("CHF", "Swiss Franc", "CHF", "🇨🇭", 0.90),
        CurrencyItem("CNY", "Chinese Yuan", "¥", "🇨🇳", 7.23),
        CurrencyItem("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.34)
    )

    private val currencyMap = supportedCurrencies.associateBy { it.code }

    fun getCurrency(code: String): CurrencyItem {
        return currencyMap[code] ?: supportedCurrencies.first()
    }

    fun getSymbol(code: String): String {
        return getCurrency(code).symbol
    }

    /**
     * Converts an amount from one currency to another using the exchange rate matrix.
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val fromItem = getCurrency(fromCurrency)
        val toItem = getCurrency(toCurrency)
        
        // Convert to USD first: amountInUsd = amount / fromItem.rateToUsd
        val amountInUsd = amount / fromItem.rateToUsd
        // Convert USD to target: targetAmount = amountInUsd * toItem.rateToUsd
        return amountInUsd * toItem.rateToUsd
    }

    /**
     * Formats an amount with currency symbol and 2 decimal places (or 0 decimals for JPY).
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        val item = getCurrency(currencyCode)
        val formatter = if (currencyCode.equals("JPY", ignoreCase = true)) {
            DecimalFormat("#,##0")
        } else {
            DecimalFormat("#,##0.00")
        }
        return "${item.symbol}${formatter.format(amount)}"
    }

    fun getRateString(baseCode: String, targetCode: String): String {
        val rate = convert(1.0, baseCode, targetCode)
        val df = DecimalFormat("#,##0.0000")
        return "1 $baseCode = ${df.format(rate)} $targetCode"
    }
}
