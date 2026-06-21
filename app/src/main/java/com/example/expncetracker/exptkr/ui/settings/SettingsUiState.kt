package com.example.expncetracker.exptkr.ui.settings

/**
 * Represents the UI state for the Settings screen.
 */
data class SettingsUiState(
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val accountPhotoUrl: String? = null,
    val lastSyncTime: String? = null,
    val isDarkMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBudgetAlertsEnabled: Boolean = true,
    val budgetThreshold: Float = 0.9f,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)
