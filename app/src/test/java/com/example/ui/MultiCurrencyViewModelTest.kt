package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.ExchangeRateApiResponse
import com.example.data.network.ExchangeRateApiService
import com.example.data.repository.ExchangeRateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiCurrencyViewModelTest {

    private class FakeExchangeRateApiService : ExchangeRateApiService {
        override suspend fun getLatestRates(baseCurrency: String): ExchangeRateApiResponse {
            return ExchangeRateApiResponse(
                result = "success",
                provider = "test-provider",
                baseCode = baseCurrency,
                timeLastUpdateUtc = "Tue, 08 Sep 2026 12:00:00 +0000",
                rates = mapOf(
                    "USD" to 1.0,
                    "EUR" to 0.90,
                    "GBP" to 0.80,
                    "JPY" to 150.0
                )
            )
        }
    }

    @Test
    fun testExchangeRateRepositoryWithApi() = runTest {
        val fakeApi = FakeExchangeRateApiService()
        val repo = ExchangeRateRepository(fakeApi)

        val ratesData = repo.getExchangeRates("USD")
        assertTrue(ratesData.isLive)
        assertEquals("USD", ratesData.baseCode)
        assertEquals(0.90, ratesData.rates["EUR"] ?: 0.0, 0.001)
        assertEquals(150.0, ratesData.rates["JPY"] ?: 0.0, 0.001)

        val convertedEur = repo.convert(100.0, "USD", "EUR", ratesData.rates)
        assertEquals(90.0, convertedEur, 0.01)

        val convertedJpy = repo.convert(10.0, "USD", "JPY", ratesData.rates)
        assertEquals(1500.0, convertedJpy, 0.01)
    }

    @Test
    fun testExchangeRateRepositoryOfflineFallback() = runTest {
        // Failing API service to test offline fallback
        val failingApi = object : ExchangeRateApiService {
            override suspend fun getLatestRates(baseCurrency: String): ExchangeRateApiResponse {
                throw RuntimeException("Network offline")
            }
        }
        val repo = ExchangeRateRepository(failingApi)
        val ratesData = repo.getExchangeRates("USD")
        assertFalse(ratesData.isLive)
        assertEquals("Offline Standard Rates", ratesData.lastUpdatedUtc)
        assertTrue(ratesData.rates.containsKey("EUR"))
        assertTrue(ratesData.rates.containsKey("JPY"))

        val convertedEur = repo.convert(100.0, "USD", "EUR", ratesData.rates)
        assertTrue(convertedEur > 0.0)
    }

    @Test
    fun testMultiCurrencyViewModelInitialization() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MultiCurrencyViewModel(app)

        assertNotNull(viewModel.uiState.value)
        assertTrue(viewModel.uiState.value.availableCurrencies.isNotEmpty())
        assertTrue(viewModel.uiState.value.selectedTargetCurrencies.isNotEmpty())

        // Test custom amount conversion
        viewModel.setCustomAmount("200")
        assertEquals("200", viewModel.uiState.value.customAmountInput)

        // Test toggling currencies
        val initialCount = viewModel.uiState.value.selectedTargetCurrencies.size
        viewModel.toggleTargetCurrency("EUR")
        assertEquals(initialCount - 1, viewModel.uiState.value.selectedTargetCurrencies.size)

        viewModel.selectAllCurrencies()
        assertEquals(viewModel.uiState.value.availableCurrencies.size, viewModel.uiState.value.selectedTargetCurrencies.size)
    }
}
