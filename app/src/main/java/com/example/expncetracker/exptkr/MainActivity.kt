package com.example.expncetracker.exptkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricResult
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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
            val preferences = dataStore.data.first()
            val biometricEnabled = preferences[BIOMETRIC_ENABLED_KEY] ?: false
            val biometricStatus = biometricAuthManager.checkBiometricAvailability()

            if (biometricEnabled && biometricStatus.isAvailable) {
                setContent {
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
                        ExpncetrackerTheme {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                AppNavGraph()
                            }
                        }
                    }
                }
            } else {
                setContent {
                    ExpncetrackerTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            AppNavGraph()
                        }
                    }
                }
            }
        }
    }
}
