package com.example.expncetracker.exptkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.example.expncetracker.exptkr.ui.navigation.AppNavGraph
import com.example.expncetracker.exptkr.ui.theme.ExpncetrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle runtime authorization changes here if necessary
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SmsPermissionManager.hasPermissions(this)) {
            permissionLauncher.launch(SmsPermissionManager.permissions)
        }

        setContent {
            ExpncetrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavGraph()
                }
            }
        }
    }
}
