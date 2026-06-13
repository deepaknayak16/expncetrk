package com.example.expncetracker.exptkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricResult
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val preferencesFlow = dataStore.data
            val biometricEnabled = preferencesFlow.map { it[BIOMETRIC_ENABLED_KEY] ?: false }
            val darkModeFlow = preferencesFlow.map { it[DARK_MODE_KEY] ?: false }
            
            val biometricStatus = biometricAuthManager.checkBiometricAvailability()

            setContent {
                val isDarkMode by darkModeFlow.collectAsState(initial = isSystemInDarkTheme())
                val isBiometricEnabled by biometricEnabled.collectAsState(initial = false)
                
                if (isBiometricEnabled && biometricStatus.isAvailable) {
                    var authenticated by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        biometricAuthManager.authenticate(this@MainActivity)
                            .collect { result ->
                                when (result) {
                                    is BiometricResult.Success -> authenticated = true
                                    else -> { /* keep showing auth or finish() */ }
                                }
                            }
                    }
                    if (authenticated) {
                        ExpncetrackerTheme(darkTheme = isDarkMode) {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                AppNavGraph()
                            }
                        }
                    }
                } else {
                    ExpncetrackerTheme(darkTheme = isDarkMode) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            AppNavGraph()
                        }
                    }
                }
            }
        }
    }
}
