package com.example.expncetracker.exptkr.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expncetracker.exptkr.ui.dashboard.DashboardScreen
import com.example.expncetracker.exptkr.ui.dashboard.DashboardViewModel
import com.example.expncetracker.exptkr.ui.settings.SettingsScreen
import com.example.expncetracker.exptkr.ui.settings.SettingsViewModel
import com.example.expncetracker.exptkr.ui.transactions.TransactionScreen
import com.example.expncetracker.exptkr.ui.transactions.TransactionViewModel
import com.example.expncetracker.exptkr.ui.addtransaction.AddTransactionScreen
import com.example.expncetracker.exptkr.ui.theme.LightBackground
import com.example.expncetracker.exptkr.ui.theme.LightPrimary
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Money",
                            color = LightTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Wise",
                            color = LightPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (currentRoute != "transactions") {
                            navController.navigate("transactions")
                        }
                    }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = LightTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightSurface,
                    titleContentColor = LightTextPrimary,
                    actionIconContentColor = LightTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = LightSurface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("dashboard", "Home", Icons.Default.Dashboard),
                    Triple("transactions", "Ledger", Icons.AutoMirrored.Filled.ListAlt),
                    Triple("settings", "Settings", Icons.Default.Settings)
                )
                
                items.forEach { (route, label, icon) ->
                    val isSelected = currentRoute == route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { 
                            Icon(
                                imageVector = icon, 
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LightPrimary,
                            unselectedIconColor = LightTextSecondary,
                            selectedTextColor = LightPrimary,
                            unselectedTextColor = LightTextSecondary,
                            indicatorColor = LightPrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    navController.navigate("add_transaction")
                },
                containerColor = LightPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "dashboard", Modifier.padding(innerPadding)) {
            composable("dashboard") {
                val vm: DashboardViewModel = hiltViewModel()
                DashboardScreen(vm)
            }
            composable("transactions") {
                val vm: TransactionViewModel = hiltViewModel()
                TransactionScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(vm)
            }
            composable("add_transaction") {
                AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
