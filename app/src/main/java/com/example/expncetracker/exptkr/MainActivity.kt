package com.example.expncetracker.exptkr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.expncetracker.exptkr.core.common.BIOMETRIC_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.DARK_MODE_KEY
import com.example.expncetracker.exptkr.core.common.PERMISSION_RATIONALE_SHOWN_KEY
import com.example.expncetracker.exptkr.core.common.SMS_PERMISSION_PERMANENTLY_DENIED_KEY
import com.example.expncetracker.exptkr.core.common.NOTIFICATION_PERMISSION_SHOWN_KEY
import com.example.expncetracker.exptkr.core.common.WAS_RATIONALE_NEEDED_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.security.BiometricResult
import com.example.expncetracker.exptkr.core.notification.AppNotificationManager
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val currentStartRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        currentStartRoute.value = intent.getStringExtra("navigate_to")

        val preferencesFlow = dataStore.data
        val biometricEnabled = preferencesFlow.map { it[BIOMETRIC_ENABLED_KEY] ?: false }
        val darkModeFlow = preferencesFlow.map { it[DARK_MODE_KEY] }
        val rationaleShownFlow = preferencesFlow.map { it[PERMISSION_RATIONALE_SHOWN_KEY] ?: false }
        val notificationRationaleFlow = preferencesFlow.map { it[NOTIFICATION_PERMISSION_SHOWN_KEY] ?: false }
        val smsPermanentlyDeniedFlow = preferencesFlow.map { it[SMS_PERMISSION_PERMANENTLY_DENIED_KEY] ?: false }
        val wasRationaleNeededFlow = preferencesFlow.map { it[WAS_RATIONALE_NEEDED_KEY] ?: false }

        setContent {
            val isDarkModePref by darkModeFlow.collectAsState(initial = null)
            val isDarkMode = isDarkModePref ?: isSystemInDarkTheme()
            val isBiometricEnabled by biometricEnabled.collectAsState(initial = false)
            val isRationaleShown by rationaleShownFlow.collectAsState(initial = false)
            val isNotificationRationaleShown by notificationRationaleFlow.collectAsState(initial = false)
            val isSmsPermanentlyDenied by smsPermanentlyDeniedFlow.collectAsState(initial = false)
            val wasRationaleNeeded by wasRationaleNeededFlow.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            var hasAskedNotificationThisSession by remember { mutableStateOf(false) }

            val startRoute by currentStartRoute

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
                // Allow partial permission - degrade gracefully:
                val hasReadSms = result[Manifest.permission.READ_SMS] == true
                val hasReceiveSms = result[Manifest.permission.RECEIVE_SMS] == true

                if (hasReadSms) {
                    // Enable historical import
                    scope.launch { dataStore.edit { it[PERMISSION_RATIONALE_SHOWN_KEY] = true } }
                }

                if (hasReadSms && hasReceiveSms) {
                    Toast.makeText(this@MainActivity, "SMS permissions granted", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        dataStore.edit { 
                            it[PERMISSION_RATIONALE_SHOWN_KEY] = true 
                            it[SMS_PERMISSION_PERMANENTLY_DENIED_KEY] = false
                        }
                    }
                }
                // WHY: Detect true permanent denial: user checked "Don't ask again".
                if (result[Manifest.permission.READ_SMS] == false &&
                    wasRationaleNeeded &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)
                ) {
                    scope.launch {
                        dataStore.edit { it[SMS_PERMISSION_PERMANENTLY_DENIED_KEY] = true }
                    }
                }
                if (result[Manifest.permission.POST_NOTIFICATIONS] == true) {
                    scope.launch {
                        dataStore.edit { it[NOTIFICATION_PERMISSION_SHOWN_KEY] = true }
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    AppNotificationManager.showQuickActionsNotification(this@MainActivity)
                }
            }
            LaunchedEffect(isNotificationRationaleShown) {
                if (!isNotificationRationaleShown &&
                    !hasAskedNotificationThisSession &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    hasAskedNotificationThisSession = true
                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
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
                            scope.launch { 
                                dataStore.edit { it[WAS_RATIONALE_NEEDED_KEY] = shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) }
                                permissionLauncher.launch(SmsPermissionManager.permissions)
                                showPermissionRationale = false
                            }
                        }) { Text("Grant Access") }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showPermissionRationale = false
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
                        delay(300)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentStartRoute.value = intent.getStringExtra("navigate_to")
    }
}
