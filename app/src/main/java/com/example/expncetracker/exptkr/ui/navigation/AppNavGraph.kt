package com.example.expncetracker.exptkr.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.example.expncetracker.exptkr.ui.budget.BudgetScreen
import com.example.expncetracker.exptkr.ui.budget.BudgetViewModel
import com.example.expncetracker.exptkr.ui.analytics.AnalyticsScreen
import com.example.expncetracker.exptkr.ui.analytics.AnalyticsViewModel
import com.example.expncetracker.exptkr.ui.accounts.AccountsScreen
import com.example.expncetracker.exptkr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val isDarkTheme = MaterialTheme.isDark

    val showBottomBar = currentRoute != "add_transaction"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AnimatedVisibility(
                visible = currentRoute != "add_transaction",
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ModernTopAppBar(
                    title = if (currentRoute == "transactions") "Statement Ledger" else "MoneyWise",
                    showSearch = currentRoute == "transactions",
                    showBack = currentRoute == "transactions",
                    onBackClick = { navController.popBackStack() },
                    onSearchClick = {
                        if (currentRoute != "transactions") {
                            navController.navigate("transactions")
                        }
                    },
                    isDarkTheme = isDarkTheme
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ModernNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ModernFab(
                    onClick = { navController.navigate("add_transaction") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                val vm: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToAddTransaction = {
                        navController.navigate("add_transaction")
                    },
                    onNavigateToStatementLedger = {
                        navController.navigate("transactions")
                    }
                )
            }
            composable("transactions") {
                val vm: TransactionViewModel = hiltViewModel()
                TransactionScreen(vm)
            }
            composable("accounts") {
                val vm: DashboardViewModel = hiltViewModel()
                AccountsScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(vm)
            }
            composable("analytics") {
                val vm: AnalyticsViewModel = hiltViewModel()
                AnalyticsScreen(vm)
            }
            composable("budget") {
                val vm: BudgetViewModel = hiltViewModel()
                BudgetScreen(vm)
            }
            composable("add_transaction") {
                AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun ModernTopAppBar(
    title: String,
    showSearch: Boolean = false,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars) // Added status bar padding to prevent overlap
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(end = 12.dp).size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                )
            }

            if (showSearch) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernNavigationBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?
) {
    val items = listOf(
        NavigationItem("dashboard", "Home", Icons.Default.Home, Icons.Outlined.Home),
        NavigationItem("analytics", "Analytics", Icons.Default.BarChart, Icons.Outlined.BarChart),
        NavigationItem("accounts", "Accounts", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
        NavigationItem("budget", "Budgets", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance),
        NavigationItem("settings", "Settings", Icons.Default.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun ModernFab(
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add Expense") }
    )
}
