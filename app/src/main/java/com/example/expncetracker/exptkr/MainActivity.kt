package com.example.expncetracker.exptkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricResult
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpnceTrkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // FIX #3: Biometric launch gate
        // In production, read this from DataStore / SharedPreferences
        val biometricEnabled = false // TODO: read from preference store
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
                    ExpnceTrkTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            AppNavGraph()
                        }
                    }
                }
            }
        } else {
            setContent {
                ExpnceTrkTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
