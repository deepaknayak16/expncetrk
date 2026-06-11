package com.example.expncetracker.exptkr.ui.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.usecase.ExportBackupUseCase
import com.example.expncetracker.exptkr.domain.usecase.ImportBackupUseCase
import com.example.expncetracker.exptkr.domain.usecase.LoadSampleDataUseCase
import com.example.expncetracker.exptkr.domain.usecase.RestoreBackupFromGoogleDriveUseCase
import com.example.expncetracker.exptkr.domain.usecase.SyncBackupToGoogleDriveUseCase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val syncBackupToGoogleDriveUseCase: SyncBackupToGoogleDriveUseCase,
    private val restoreBackupFromGoogleDriveUseCase: RestoreBackupFromGoogleDriveUseCase
) : ViewModel() {

    private val _statusEvent = MutableSharedFlow<String>()
    val statusEvent = _statusEvent.asSharedFlow()
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

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
    
    fun signInToGoogle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // In a real implementation, this would trigger the Google Sign-In flow
                // For now, we'll simulate a successful sign-in
                // The actual implementation would require an Activity context
                _uiState.value = _uiState.value.copy(
                    isSignedIn = true,
                    accountName = "user@gmail.com", // This would come from Google Sign-In result
                    isLoading = false
                )
                //Launch real Google Sign-In flow here
                _statusEvent.emit("Signed in to Google successfully")
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
            _uiState.value = _uiState.value.copy(
                isSignedIn = false,
                accountName = null
            )
            _statusEvent.emit("Signed out from Google")
        }
    }
}
