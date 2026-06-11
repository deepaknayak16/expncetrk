package com.example.expncetracker.exptkr

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle runtime authorization changes here if necessary
    }

    private val _uiStateFlow = MutableStateFlow(false)
    val uiStateFlow: StateFlow<Boolean> = _uiStateFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SmsPermissionManager.hasPermissions(this)) {
            permissionLauncher.launch(SmsPermissionManager.permissions)
        }

        // Observe dark mode setting
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStore.data.collect { preferences ->
                    _uiStateFlow.value = preferences[DARK_MODE_KEY] ?: false
                }
            }
        }

        setContent {
            val isDarkMode by uiStateFlow.collectAsState()
            ExpncetrackerTheme(darkTheme = isDarkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavGraph()
                }
            }
        }
    }
}
