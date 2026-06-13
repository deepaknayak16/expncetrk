package com.example.expncetracker.exptkr.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.export.CsvExporter
import com.example.expncetracker.exptkr.data.export.PdfExporter
import com.example.expncetracker.exptkr.domain.repository.GoogleDriveSyncRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleDriveSyncRepository: GoogleDriveSyncRepository,
    private val transactionRepository: TransactionRepository,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            _uiState.update {
                it.copy(
                    isSignedIn = account != null,
                    accountName = account?.displayName
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
            try {
                googleDriveSyncRepository.syncToDrive()
                _statusEvent.send("Sync completed successfully")
            } catch (e: Exception) {
                _statusEvent.send("Sync failed: ${e.message}")
            }
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _statusEvent.send("Restoring from Google Drive...")
            try {
                googleDriveSyncRepository.restoreFromDrive()
                _statusEvent.send("Restore completed successfully")
            } catch (e: Exception) {
                _statusEvent.send("Restore failed: ${e.message}")
            }
        }
    }

    // FIX #4: Export CSV
    fun exportCsv(csvExporter: CsvExporter, onShare: (File) -> Unit) {
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
    fun exportPdf(pdfExporter: PdfExporter, onShare: (File) -> Unit) {
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
        _uiState.update { it.copy(isDarkMode = enabled) }
        // TODO: persist to DataStore
    }

    // FIX #3: Biometric toggle
    fun toggleBiometric(enabled: Boolean) {
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
        // TODO: persist to DataStore
    }

    fun loadMockData() {
        viewModelScope.launch {
            try {
                transactionRepository.loadMockData()
                _statusEvent.send("Demo data loaded successfully")
            } catch (e: Exception) {
                _statusEvent.send("Failed to load demo data: ${e.message}")
            }
        }
    }
}

data class SettingsUiState(
    val isSignedIn: Boolean = false,
    val accountName: String? = null,
    val isDarkMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lastSyncTime: String? = null
)
