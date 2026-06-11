package com.example.expncetracker.exptkr.ui.settings

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.example.expncetracker.exptkr.domain.usecase.ExportBackupUseCase
import com.example.expncetracker.exptkr.domain.usecase.ImportBackupUseCase
import com.example.expncetracker.exptkr.domain.usecase.LoadSampleDataUseCase
import com.example.expncetracker.exptkr.domain.usecase.RestoreBackupFromGoogleDriveUseCase
import com.example.expncetracker.exptkr.domain.usecase.SyncBackupToGoogleDriveUseCase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val syncBackupToGoogleDriveUseCase: SyncBackupToGoogleDriveUseCase,
    private val restoreBackupFromGoogleDriveUseCase: RestoreBackupFromGoogleDriveUseCase,
    private val googleSignInClient: GoogleSignInClient,
    private val application: android.app.Application
) : ViewModel() {

    private val _statusEvent = MutableSharedFlow<String>()
    val statusEvent = _statusEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkLastSignedInAccount()
        loadDarkModeSetting()
    }

    private fun loadDarkModeSetting() {
        viewModelScope.launch {
            val isDarkMode = application.dataStore.data.first()[DARK_MODE_KEY] ?: false
            _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
        }
    }

    private fun checkLastSignedInAccount() {
        val account = GoogleSignIn.getLastSignedInAccount(googleSignInClient.applicationContext)
        if (account != null) {
            _uiState.value = _uiState.value.copy(
                isSignedIn = true,
                accountName = account.email
            )
        }
    }

    fun loadMockData() {
        viewModelScope.launch {
            loadSampleDataUseCase.execute()
            _statusEvent.emit("Sample data loaded successfully")
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val path = exportBackupUseCase.execute()
            _statusEvent.emit("Backup saved to: $path")
        }
    }

    fun importBackup() {
        viewModelScope.launch {
            if (importBackupUseCase.execute()) {
                _statusEvent.emit("Data restored successfully")
            } else {
                _statusEvent.emit("Backup file not found")
            }
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(Exception::class.java)

                if (account != null) {
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = true,
                        accountName = account.email,
                        isLoading = false
                    )
                    _statusEvent.emit("Signed in to Google successfully")
                } else {
                    throw Exception("Account was null")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                _statusEvent.emit("Sign in failed: ${e.message}")
            }
        }
    }

    fun syncToGoogleDrive() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val accountName = _uiState.value.accountName
            if (accountName == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _statusEvent.emit("Please sign in first")
                return@launch
            }

            val result = syncBackupToGoogleDriveUseCase.execute(accountName)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { message ->
                    _statusEvent.emit(message)
                },
                onFailure = { error ->
                    _statusEvent.emit("Sync failed: ${error.message}")
                }
            )
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val accountName = _uiState.value.accountName
            if (accountName == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _statusEvent.emit("Please sign in first")
                return@launch
            }

            val result = restoreBackupFromGoogleDriveUseCase.execute(accountName)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { message ->
                    _statusEvent.emit(message)
                },
                onFailure = { error ->
                    _statusEvent.emit("Restore failed: ${error.message}")
                }
            )
        }
    }

    fun signOutFromGoogle() {
        viewModelScope.launch {
            googleSignInClient.signOut().addOnCompleteListener {
                _uiState.value = _uiState.value.copy(
                    isSignedIn = false,
                    accountName = null
                )
                viewModelScope.launch {
                    _statusEvent.emit("Signed out from Google")
                }
            }
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            application.dataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = isDark
            }
            _uiState.value = _uiState.value.copy(isDarkMode = isDark)
            _statusEvent.emit(if (isDark) "Dark mode enabled" else "Light mode enabled")
        }
    }
}