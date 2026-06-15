package com.example.expncetracker.exptkr.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Biometric authentication manager for handling fingerprint/face unlock authentication.
 * Supports biometric prompt with customizable title, subtitle, and negative button text.
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Checks if biometric authentication is available on the device.
     * Returns [BiometricStatus] indicating availability and type.
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            else -> BiometricStatus.Unavailable
        }
    }

    /**
     * Shows biometric prompt using the provided activity.
     * Returns a Flow that emits authentication results.
     */
    fun authenticate(
        activity: androidx.fragment.app.FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Use your biometric or PIN/Pattern to continue"
    ): Flow<BiometricResult> = callbackFlow {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    trySend(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    trySend(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    trySend(BiometricResult.Error(errString.toString()))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)

        awaitClose {
            biometricPrompt.cancelAuthentication()
        }
    }
}

/**
 * Status of biometric authentication availability.
 */
sealed class BiometricStatus {
    object Available : BiometricStatus()
    object NoHardware : BiometricStatus()
    object HardwareUnavailable : BiometricStatus()
    object NoneEnrolled : BiometricStatus()
    object Unavailable : BiometricStatus()

    val isAvailable: Boolean
        get() = this is Available

    val message: String
        get() = when (this) {
            is Available -> "Biometric authentication is available"
            is NoHardware -> "No biometric hardware found on this device"
            is HardwareUnavailable -> "Biometric hardware is currently unavailable"
            is NoneEnrolled -> "No biometric credentials enrolled. Please set up fingerprint or face unlock."
            is Unavailable -> "Biometric authentication is not available"
        }
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    object Success : BiometricResult()
    object Failed : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}
