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
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Text(
            text = "Profile & Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(24.dp))

        SettingsGroup(title = "Google Drive Sync") {
            if (uiState.isSignedIn) {
                Text(
                    text = "Signed in as: ${uiState.accountName ?: "Unknown"}",
                    color = Color(0xFF4ADE80),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            SettingsItem(
                label = "Sign In to Google",
                icon = Icons.Default.CloudUpload,
                onClick = { viewModel.signInToGoogle() }
            )
            
            if (uiState.isSignedIn) {
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
                SettingsItem(
                    label = "Sign Out",
                    icon = Icons.Default.RestartAlt,
                    onClick = { viewModel.signOutFromGoogle() }
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Load Demo Banking Data", fontWeight = FontWeight.Bold)
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
            color = Color(0xFF3B82F6),
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
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.White, fontSize = 15.sp)
        }
    }
}
