package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CurrencyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Finance Tracker", appName)
  }

  @Test
  fun `test currency conversion and formatting`() {
    val usdToEur = CurrencyManager.convert(100.0, "USD", "EUR")
    assertEquals(92.0, usdToEur, 0.01)

    val formatted = CurrencyManager.formatAmount(1250.50, "USD")
    assertTrue(formatted.contains("1,250.50") || formatted.contains("1250.50"))
  }
}

