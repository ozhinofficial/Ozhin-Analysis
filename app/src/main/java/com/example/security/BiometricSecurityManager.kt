package com.example.security

import android.content.Context
import androidx.fragment.app.FragmentActivity

object BiometricSecurityManager {

    private const val PREFS_NAME = "finance_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_PIN_CODE = "security_pin_code"

    fun isBiometricSupported(context: Context): Boolean {
        return BiometricAuthenticator.getInstance(context).canAuthenticate()
    }

    fun getBiometricStatus(context: Context): BiometricStatus {
        return BiometricAuthenticator.getInstance(context).checkBiometricStatus()
    }

    fun isSecurityEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bioEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val pin = prefs.getString(KEY_PIN_CODE, "") ?: ""
        return bioEnabled || pin.isNotEmpty()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun getPin(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PIN_CODE, "") ?: ""
    }

    fun setPin(context: Context, pin: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_CODE, pin)
            .apply()
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val saved = getPin(context)
        return saved.isEmpty() || saved == enteredPin
    }

    /**
     * Prompts the user with the system BiometricPrompt dialog using BiometricAuthenticator.
     */
    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Unlock Ozhin Finance Tracker to access your secure records",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BiometricAuthenticator.getInstance(activity).authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = { onSuccess() },
            onError = { _, err -> onError(err.toString()) },
            onFailed = { onError("Biometric authentication failed. Try again or enter PIN.") }
        )
    }
}
