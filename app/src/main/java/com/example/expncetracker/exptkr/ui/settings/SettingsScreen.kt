package com.example.expncetracker.exptkr.ui.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                    onClick = { signInLauncher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sign In to Google", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Data Management Section — FIX #4: Add export buttons
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
                // FIX #4: Export CSV
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
                // FIX #4: Export PDF
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
                
                if (uiState.isExporting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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

                // FIX #3: Biometric lock toggle
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
                    label = "Budget Alerts",
                    subtitle = if (uiState.isBudgetAlertsEnabled) "Notify at ${(uiState.budgetThreshold * 100).toInt()}% spent" else "Disabled",
                    icon = Icons.Default.NotificationsActive,
                    trailingContent = {
                        Switch(
                            checked = uiState.isBudgetAlertsEnabled,
                            onCheckedChange = { viewModel.toggleBudgetAlerts(it) }
                        )
                    },
                    onClick = { viewModel.toggleBudgetAlerts(!uiState.isBudgetAlertsEnabled) }
                )

                if (uiState.isBudgetAlertsEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Alert Threshold: ${(uiState.budgetThreshold * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = uiState.budgetThreshold,
                            onValueChange = { viewModel.updateBudgetThreshold(it) },
                            valueRange = 0.5f..1.0f,
                            steps = 9
                        )
                    }
                }
            }
        }

        // Danger Zone
        item {
            var showConfirmDialog by remember { mutableStateOf(false) }
            
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Load Demo Data?") },
                    text = { Text("This will replace all your current transactions with mock sample data for testing. This action cannot be undone easily.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.loadMockData()
                                showConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            SectionHeader(title = "Danger Zone", color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Load Demo Data", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    onClick = { }
                )
                SettingsPreferenceItem(
                    label = "Privacy Policy",
                    icon = Icons.Default.Policy,
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/expncetracker/privacy-policy".toUri())
                        ctx.startActivity(intent)
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

private fun shareFile(context: android.content.Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
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
