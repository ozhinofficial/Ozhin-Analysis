package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BiometricAuthenticatorTest {

    @Test
    fun testBiometricAuthenticatorInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authenticator = BiometricAuthenticator.getInstance(context)
        assertNotNull(authenticator)
    }

    @Test
    fun testBiometricStatusAvailabilityLogic() {
        assertTrue(BiometricStatus.Ready.isAvailable)
        assertFalse(BiometricStatus.NoHardware.isAvailable)
        assertFalse(BiometricStatus.NoneEnrolled.isAvailable)
        assertFalse(BiometricStatus.HardwareUnavailable.isAvailable)
        assertFalse(BiometricStatus.SecurityUpdateRequired.isAvailable)
        assertFalse(BiometricStatus.Unknown(-1).isAvailable)
    }

    @Test
    fun testCheckBiometricStatusReturnsValidState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authenticator = BiometricAuthenticator(context)
        val status = authenticator.checkBiometricStatus()
        assertNotNull(status)
    }

    @Test
    fun testBiometricResultModels() {
        val success = BiometricResult.Success(null)
        val failed = BiometricResult.Failed
        val cancelled = BiometricResult.Cancelled
        val error = BiometricResult.Error(10, "Fingerprint sensor locked out")

        assertNotNull(success)
        assertNotNull(failed)
        assertNotNull(cancelled)
        assertNotNull(error)
        assertTrue(error.errorMessage.contains("locked out"))
    }
}
