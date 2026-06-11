package com.example.expncetracker.exptkr.ui.settings

/**
 * Represents the UI state for the Settings screen.
 */
data class SettingsUiState(
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val lastSyncTime: String? = null,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
