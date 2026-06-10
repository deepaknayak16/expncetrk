package com.example.expncetracker.exptkr.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightBackground
import com.example.expncetracker.exptkr.ui.theme.LightBorder
import com.example.expncetracker.exptkr.ui.theme.LightCardBackground
import com.example.expncetracker.exptkr.ui.theme.LightPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val ctx = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value

    // Google account picker launcher
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { _ ->
        // Handle account selection result
    }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Profile & Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LightTextPrimary,
        )

        Spacer(Modifier.height(24.dp))

        // Account Sign In Section
        SettingsGroup(title = "Account") {
            if (uiState.isSignedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = LightPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Signed In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary
                            )
                            Text(
                                text = uiState.accountName ?: "Unknown",
                                fontSize = 13.sp,
                                color = LightTextSecondary
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { viewModel.signOutFromGoogle() }) {
                            Text("Sign Out", color = LightPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.signInToGoogle() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightPrimary)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign In to Google", fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isSignedIn) {
                Spacer(Modifier.height(12.dp))

                SettingsItem(
                    label = "Backup to Google Drive",
                    icon = Icons.Default.CloudUpload,
                    onClick = { viewModel.syncToGoogleDrive() }
                )
                SettingsItem(
                    label = "Restore from Google Drive",
                    icon = Icons.Default.CloudDownload,
                    onClick = { viewModel.restoreFromGoogleDrive() }
                )
                
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsGroup(title = "Data Management") {
            SettingsItem(
                label = "Backup to Local Storage",
                icon = Icons.Default.CloudUpload,
                onClick = { viewModel.exportBackup() }
            )
            SettingsItem(
                label = "Restore from Local Storage",
                icon = Icons.Default.CloudDownload,
                onClick = { viewModel.importBackup() }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.loadMockData() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightPrimary)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Load Demo Banking Data",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            color = LightPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = LightSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = LightPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = LightTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
