package com.example.expncetracker.exptkr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.PERMISSION_RATIONALE_SHOWN_KEY
import com.example.expncetracker.exptkr.core.common.SMS_PERMISSION_PERMANENTLY_DENIED_KEY
import com.example.expncetracker.exptkr.core.common.NOTIFICATION_PERMISSION_SHOWN_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricResult
import com.example.expncetracker.exptkr.core.notification.AppNotificationManager
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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
        val rationaleShownFlow = preferencesFlow.map { it[PERMISSION_RATIONALE_SHOWN_KEY] ?: false }
        val notificationRationaleFlow = preferencesFlow.map { it[NOTIFICATION_PERMISSION_SHOWN_KEY] ?: false }
        val smsPermanentlyDeniedFlow = preferencesFlow.map { it[SMS_PERMISSION_PERMANENTLY_DENIED_KEY] ?: false }

        setContent {
            val isDarkMode by darkModeFlow.collectAsState(initial = isSystemInDarkTheme())
            val isBiometricEnabled by biometricEnabled.collectAsState(initial = false)
            val isRationaleShown by rationaleShownFlow.collectAsState(initial = false)
            val isNotificationRationaleShown by notificationRationaleFlow.collectAsState(initial = false)
            val isSmsPermanentlyDenied by smsPermanentlyDeniedFlow.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            val startRoute = remember { intent.getStringExtra("navigate_to") }

            LaunchedEffect(Unit) {
                AppNotificationManager.showQuickActionsNotification(this@MainActivity)
            }

            var showPermissionRationale by remember(isRationaleShown, isSmsPermanentlyDenied) {
                mutableStateOf(
                    !isRationaleShown &&
                            !SmsPermissionManager.hasPermissions(this@MainActivity) &&
                            !isSmsPermanentlyDenied
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                if (result[android.Manifest.permission.READ_SMS] == true) {
                    Toast.makeText(this@MainActivity, "SMS permission granted", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        dataStore.edit { it[PERMISSION_RATIONALE_SHOWN_KEY] = true }
                    }
                    // In "Maybe Later" click:
                    scope.launch {
                        dataStore.edit { it[PERMISSION_RATIONALE_SHOWN_KEY] = true }
                        // DON'T set permanently denied here — let them try again later
                    }
                }
                if (result[android.Manifest.permission.POST_NOTIFICATIONS] == true || result[android.Manifest.permission.POST_NOTIFICATIONS] == false) {
                    scope.launch {
                        dataStore.edit { it[NOTIFICATION_PERMISSION_SHOWN_KEY] = true }
                    }
                }
            }

            LaunchedEffect(isNotificationRationaleShown) {
                if (!isNotificationRationaleShown &&
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
                }
            }

            val biometricStatus = remember { biometricAuthManager.checkBiometricAvailability() }

            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { 
                        showPermissionRationale = false
                        scope.launch {
                            dataStore.edit { it[PERMISSION_RATIONALE_SHOWN_KEY] = true }
                        }
                    },
                    title = { Text("SMS Permission Required") },
                    text = { Text("This app tracks expenses automatically by reading transaction SMS. Please grant SMS access to enable this feature.") },
                    confirmButton = {
                        Button(onClick = {
                            permissionLauncher.launch(SmsPermissionManager.permissions)
                            showPermissionRationale = false
                        }) { Text("Grant Access") }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showPermissionRationale = false
                            scope.launch {
                                dataStore.edit { it[SMS_PERMISSION_PERMANENTLY_DENIED_KEY] = true }
                            }
                        }) { Text("Maybe Later") }
                    }
                )
            }

            if (isBiometricEnabled && biometricStatus.isAvailable) {
                var authenticated by remember { mutableStateOf(false) }
                var authError by remember { mutableStateOf<String?>(null) }
                var retryCount by remember { mutableIntStateOf(0) }

                LaunchedEffect(retryCount) {
                    if (!authenticated) {
                        biometricAuthManager.authenticate(this@MainActivity)
                            .collect { result ->
                                when (result) {
                                    is BiometricResult.Success -> {
                                        authenticated = true
                                        authError = null
                                    }
                                    is BiometricResult.Error -> authError = result.message
                                    else -> { }
                                }
                            }
                    }
                }

                if (authError != null) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Authentication Required") },
                        text = { Text(authError ?: "Biometric authentication failed. Please try again.") },
                        confirmButton = {
                            Button(onClick = {
                                authError = null
                                retryCount++
                            }) { Text("Retry") }
                        },
                        dismissButton = {
                            Column {
                                TextButton(onClick = {
                                    scope.launch {
                                        dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = false }
                                        authenticated = true
                                        authError = null
                                    }
                                }) { Text("Use PIN/Fallback Instead") }
                                TextButton(onClick = { finish() }) { Text("Exit App") }
                            }
                        }
                    )
                }

                if (authenticated) {
                    ExpncetrackerTheme(darkTheme = isDarkMode) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            AppNavGraph(startRoute = startRoute)
                        }
                    }
                }
            } else {
                ExpncetrackerTheme(darkTheme = isDarkMode) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppNavGraph(startRoute = startRoute)
                    }
                }
            }
        }
    }
}
