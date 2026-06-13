package com.example.expncetracker.exptkr.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.data.export.CsvExporter
import com.example.expncetracker.exptkr.data.export.PdfExporter
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.domain.usecase.RestoreBackupFromGoogleDriveUseCase
import com.example.expncetracker.exptkr.domain.usecase.SyncBackupToGoogleDriveUseCase
import com.example.expncetracker.exptkr.domain.usecase.LoadSampleDataUseCase
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncBackupToGoogleDriveUseCase: SyncBackupToGoogleDriveUseCase,
    private val restoreBackupFromGoogleDriveUseCase: RestoreBackupFromGoogleDriveUseCase,
    private val transactionRepository: TransactionRepository,
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val googleSignInClient: GoogleSignInClient,
    private val biometricAuthManager: BiometricAuthManager,
    private val csvExporter: CsvExporter,
    private val pdfExporter: PdfExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val preferences = context.dataStore.data.first()
            val isDark = preferences[DARK_MODE_KEY] ?: false
            val isBiometric = preferences[BIOMETRIC_ENABLED_KEY] ?: false
            val biometricStatus = biometricAuthManager.checkBiometricAvailability()

            _uiState.update {
                it.copy(
                    isSignedIn = account != null,
                    accountName = account?.displayName,
                    isDarkMode = isDark,
                    isBiometricEnabled = isBiometric,
                    isBiometricAvailable = biometricStatus is BiometricStatus.Available
                )
            }
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
                _uiState.update {
                    it.copy(isSignedIn = true, accountName = account?.displayName)
                }
                _statusEvent.send("Signed in successfully")
            } catch (e: Exception) {
                _statusEvent.send("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOutFromGoogle() {
        viewModelScope.launch {
            googleSignInClient.signOut().addOnCompleteListener {
                _uiState.update { it.copy(isSignedIn = false, accountName = null) }
            }
        }
    }

    fun syncToGoogleDrive() {
        viewModelScope.launch {
            _statusEvent.send("Syncing to Google Drive...")
            syncBackupToGoogleDriveUseCase.execute().fold(
                onSuccess = { msg -> _statusEvent.send(msg) },
                onFailure = { e -> _statusEvent.send("Sync failed: ${e.message}") }
            )
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _statusEvent.send("Restoring from Google Drive...")
            restoreBackupFromGoogleDriveUseCase.execute().fold(
                onSuccess = { msg -> _statusEvent.send(msg) },
                onFailure = { e -> _statusEvent.send("Restore failed: ${e.message}") }
            )
        }
    }

    // FIX #4: Export CSV
    fun exportCsv(onShare: (File) -> Unit) {
        viewModelScope.launch {
            _statusEvent.send("Generating CSV...")
            csvExporter.exportToCsv()
                .onSuccess { file ->
                    _statusEvent.send("CSV exported successfully")
                    onShare(file)
                }
                .onFailure { e ->
                    _statusEvent.send("Export failed: ${e.message}")
                }
        }
    }

    // FIX #4: Export PDF
    fun exportPdf(onShare: (File) -> Unit) {
        viewModelScope.launch {
            _statusEvent.send("Generating PDF...")
            pdfExporter.exportToPdf()
                .onSuccess { file ->
                    _statusEvent.send("PDF exported successfully")
                    onShare(file)
                }
                .onFailure { e ->
                    _statusEvent.send("Export failed: ${e.message}")
                }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = enabled
            }
            _uiState.update { it.copy(isDarkMode = enabled) }
        }
    }

    // FIX #3: Biometric toggle
    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[BIOMETRIC_ENABLED_KEY] = enabled
            }
            _uiState.update { it.copy(isBiometricEnabled = enabled) }
            _statusEvent.send(if (enabled) "Biometric lock enabled" else "Biometric lock disabled")
        }
    }

    fun loadMockData() {
        viewModelScope.launch {
            try {
                loadSampleDataUseCase.execute()
                _statusEvent.send("Demo data loaded successfully")
            } catch (e: Exception) {
                _statusEvent.send("Failed to load demo data: ${e.message}")
            }
        }
    }
}
