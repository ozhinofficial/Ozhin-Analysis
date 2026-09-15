package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Detailed representation of biometric hardware & enrollment availability.
 */
sealed class BiometricStatus {
    object Ready : BiometricStatus()
    object NoneEnrolled : BiometricStatus()
    object NoHardware : BiometricStatus()
    object HardwareUnavailable : BiometricStatus()
    object SecurityUpdateRequired : BiometricStatus()
    data class Unknown(val code: Int) : BiometricStatus()

    val isAvailable: Boolean get() = this is Ready
}

/**
 * Result returned from a biometric authentication attempt.
 */
sealed class BiometricResult {
    data class Success(val result: BiometricPrompt.AuthenticationResult?) : BiometricResult()
    data class Error(val errorCode: Int, val errorMessage: String) : BiometricResult()
    object Failed : BiometricResult()
    object Cancelled : BiometricResult()
}

/**
 * BiometricAuthenticator
 *
 * Utility class utilizing the [androidx.biometric] library to authenticate users
 * via Fingerprint or Face Recognition to secure app launch and sensitive records.
 */
class BiometricAuthenticator(private val context: Context) {

    private val biometricManager: BiometricManager = BiometricManager.from(context)

    companion object {
        const val DEFAULT_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

        @Volatile
        private var INSTANCE: BiometricAuthenticator? = null

        fun getInstance(context: Context): BiometricAuthenticator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricAuthenticator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Inspects whether biometric sensors (fingerprint, face recognition) are present and registered.
     */
    fun checkBiometricStatus(
        authenticators: Int = DEFAULT_AUTHENTICATORS
    ): BiometricStatus {
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            else -> BiometricStatus.Unknown(-1)
        }
    }

    /**
     * Returns true if fingerprint or face recognition is supported and enrolled on this device.
     */
    fun canAuthenticate(authenticators: Int = DEFAULT_AUTHENTICATORS): Boolean {
        return checkBiometricStatus(authenticators).isAvailable
    }

    /**
     * Displays the standard Android BiometricPrompt for fingerprint or face recognition to secure app launch.
     *
     * @param activity The host [FragmentActivity] required by [BiometricPrompt].
     * @param title Title displayed at top of the system prompt dialog.
     * @param subtitle Subtitle giving context (e.g. app name).
     * @param description Brief instruction for the user.
     * @param negativeButtonText Text for the cancel or fallback button.
     * @param onSuccess Callback invoked when fingerprint or face recognition is confirmed.
     * @param onError Callback invoked upon error (e.g. cancelled, lockout, sensor unavailable).
     * @param onFailed Callback invoked when a biometric read fails (unrecognized fingerprint/face).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Unlock Ozhin Finance Tracker",
        description: String = "Confirm your fingerprint or face recognition to access your financial records.",
        negativeButtonText: String = "Use PIN / Cancel",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(DEFAULT_AUTHENTICATORS)
            .build()

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptInfo)
    }

    /**
     * Dedicated convenience method tailored specifically for securing app launch.
     */
    fun authenticateForAppLaunch(
        activity: FragmentActivity,
        onAuthenticated: () -> Unit,
        onFallbackToPasscode: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticate(
            activity = activity,
            title = "App Launch Verification",
            subtitle = "Ozhin Finance Tracker is locked",
            description = "Touch the fingerprint sensor or glance at the camera to unlock.",
            negativeButtonText = "Enter PIN",
            onSuccess = { onAuthenticated() },
            onError = { code, err ->
                if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON || code == BiometricPrompt.ERROR_USER_CANCELED) {
                    onFallbackToPasscode()
                } else {
                    onError(err.toString())
                }
            },
            onFailed = {
                onError("Biometric not recognized. Please try again or enter your PIN.")
            }
        )
    }

    /**
     * Suspendable coroutine extension to authenticate asynchronously.
     */
    suspend fun authenticateAsync(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Unlock Ozhin Finance Tracker",
        negativeButtonText: String = "Cancel"
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            onSuccess = { result ->
                if (continuation.isActive) continuation.resume(BiometricResult.Success(result))
            },
            onError = { code, msg ->
                if (continuation.isActive) {
                    if (code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        continuation.resume(BiometricResult.Cancelled)
                    } else {
                        continuation.resume(BiometricResult.Error(code, msg.toString()))
                    }
                }
            },
            onFailed = {
                if (continuation.isActive) continuation.resume(BiometricResult.Failed)
            }
        )
    }
}
