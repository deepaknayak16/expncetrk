package com.example.expncetracker.exptkr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
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

        val preferencesFlow = dataStore.data
        val biometricEnabled = preferencesFlow.map { it[BIOMETRIC_ENABLED_KEY] ?: false }
        val darkModeFlow = preferencesFlow.map { it[DARK_MODE_KEY] ?: false }

        setContent {
            val isDarkMode by darkModeFlow.collectAsState(initial = isSystemInDarkTheme())
            val isBiometricEnabled by biometricEnabled.collectAsState(initial = false)
            
            var showPermissionRationale by remember { 
                mutableStateOf(!SmsPermissionManager.hasPermissions(this@MainActivity)) 
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                val granted = result.values.all { it }
                if (granted) {
                    Toast.makeText(this@MainActivity, "SMS permission granted", Toast.LENGTH_SHORT).show()
                }
            }

            val biometricStatus = remember { biometricAuthManager.checkBiometricAvailability() }

            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationale = false },
                    title = { Text("SMS Permission Required") },
                    text = { Text("This app tracks expenses automatically by reading transaction SMS. Please grant SMS access to enable this feature.") },
                    confirmButton = {
                        Button(onClick = {
                            permissionLauncher.launch(SmsPermissionManager.permissions)
                            showPermissionRationale = false
                        }) { Text("Grant Access") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionRationale = false }) { Text("Maybe Later") }
                    }
                )
            }

            if (isBiometricEnabled && biometricStatus.isAvailable) {
                var authenticated by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    biometricAuthManager.authenticate(this@MainActivity)
                        .collect { result ->
                            when (result) {
                                is BiometricResult.Success -> authenticated = true
                                is BiometricResult.Error -> {
                                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                else -> { }
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
