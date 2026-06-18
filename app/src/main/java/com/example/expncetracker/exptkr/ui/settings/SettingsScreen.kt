package com.example.expncetracker.exptkr.ui.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.expncetracker.BuildConfig
import com.example.expncetracker.exptkr.core.common.Constants.FILE_PROVIDER_AUTHORITY
import com.example.expncetracker.exptkr.data.export.CsvExporter
import com.example.expncetracker.exptkr.data.export.PdfExporter
import com.example.expncetracker.exptkr.security.BiometricAuthManager
import com.example.expncetracker.exptkr.ui.components.SettingsPreferenceItem
import com.example.expncetracker.exptkr.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val ctx = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    var isSigningIn by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        when (result.resultCode) {
            Activity.RESULT_OK -> viewModel.handleSignInResult(result.data)
            Activity.RESULT_CANCELED -> {
                Toast.makeText(ctx, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(ctx, "Sign-in failed (${result.resultCode})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg -> 
            if (msg.contains("failed", ignoreCase = true)) {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    if (msg.contains("CSV")) viewModel.exportCsv { file -> shareFile(ctx, file, "text/csv") }
                    else if (msg.contains("PDF")) viewModel.exportPdf { file -> shareFile(ctx, file, "application/pdf") }
                }
            } else {
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Account Section
            item {
                SectionHeader(title = "Account")
                if (uiState.isSignedIn) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Signed In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.accountName ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.signOutFromGoogle() }) {
                                Text(
                                    "Sign Out",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { 
                            isSigningIn = true
                            signInLauncher.launch(viewModel.getSignInIntent()) 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Signing in...", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Sign In to Google", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Data Management Section
            item {
                SectionHeader(title = "Data Management")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsPreferenceItem(
                        label = "Cloud Backup",
                        subtitle = if (uiState.lastSyncTime != null) "Last synced: ${uiState.lastSyncTime}" else "Sync your data to Google Drive",
                        icon = Icons.Default.CloudUpload,
                        onClick = { viewModel.syncToGoogleDrive() }
                    )
                    SettingsPreferenceItem(
                        label = "Restore from Cloud",
                        subtitle = "Import data from your Google Drive",
                        icon = Icons.Default.CloudDownload,
                        onClick = { viewModel.restoreFromGoogleDrive() }
                    )
                    SettingsPreferenceItem(
                        label = "Export as CSV",
                        subtitle = "Share transactions as a spreadsheet",
                        icon = Icons.Default.TableChart,
                        enabled = !uiState.isExporting,
                        onClick = {
                            viewModel.exportCsv { file ->
                                shareFile(ctx, file, "text/csv")
                            }
                        }
                    )
                    SettingsPreferenceItem(
                        label = "Export as PDF",
                        subtitle = "Generate a printable expense report",
                        icon = Icons.Default.PictureAsPdf,
                        enabled = !uiState.isExporting,
                        onClick = {
                            viewModel.exportPdf { file ->
                                shareFile(ctx, file, "application/pdf")
                            }
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = uiState.isExporting,
                        enter = expandVertically()  ,
                        exit = shrinkVertically()
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                    }
                    SettingsPreferenceItem(
                        label = "Dark Mode",
                        subtitle = if (uiState.isDarkMode) "Enabled" else "Disabled",
                        icon = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        trailingContent = {
                            Switch(
                                checked = uiState.isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) }
                            )
                        },
                        onClick = { viewModel.toggleDarkMode(!uiState.isDarkMode) }
                    )

                    if (uiState.isBiometricAvailable) {
                        SettingsPreferenceItem(
                            label = "Biometric Lock",
                            subtitle = if (uiState.isBiometricEnabled) "Enabled" else "Disabled",
                            icon = Icons.Default.Fingerprint,
                            trailingContent = {
                                Switch(
                                    checked = uiState.isBiometricEnabled,
                                    onCheckedChange = { viewModel.toggleBiometric(it) }
                                )
                            },
                            onClick = { viewModel.toggleBiometric(!uiState.isBiometricEnabled) }
                        )
                    }

                    SettingsPreferenceItem(
                        label = "Reset SMS Sync",
                        subtitle = "Re-enable automatic SMS tracking",
                        icon = Icons.Default.Sms,
                        onClick = { viewModel.resetSmsPermission() }
                    )
                }
            }

            // App Info Section
            item {
                SectionHeader(title = "About")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsPreferenceItem(
                        label = "Version",
                        subtitle = BuildConfig.VERSION_NAME,
                        icon = Icons.Default.Info,
                        onClick = null
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

private fun shareFile(context: android.content.Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Report"))
}

@Composable
private fun SectionHeader(title: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}
