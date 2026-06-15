package com.example.expncetracker.exptkr.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expncetracker.R
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
import com.example.expncetracker.exptkr.ui.accounts.AccountsViewModel
import com.example.expncetracker.exptkr.ui.accounts.AccountsScreen
import com.example.expncetracker.exptkr.ui.categories.CategoriesViewModel
import com.example.expncetracker.exptkr.ui.categories.CategoriesScreen
import com.example.expncetracker.exptkr.ui.goals.GoalsScreen
import com.example.expncetracker.exptkr.ui.goals.GoalsViewModel
import com.example.expncetracker.exptkr.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(startRoute: String? = null) {
    val navController = rememberNavController()
    
    LaunchedEffect(startRoute) {
        if (startRoute != null) {
            navController.navigate(startRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val accountsViewModel: AccountsViewModel = hiltViewModel()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val goalsViewModel: GoalsViewModel = hiltViewModel()

    val isFullScreenRoute = currentRoute?.startsWith("add_transaction") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isFullScreenRoute,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                val drawerItems = listOf(
                    NavigationItem("dashboard", stringResource(R.string.today), Icons.Default.Home, Icons.Outlined.Home),
                    NavigationItem("transactions", "Ledger", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
                    NavigationItem("settings", stringResource(R.string.settings_title), Icons.Default.Settings, Icons.Outlined.Settings)
                )
                
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label, style = MaterialTheme.typography.labelLarge) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            topBar = {
                if (!isFullScreenRoute) {
                    ModernTopAppBar(
                        title = when (currentRoute) {
                            "transactions" -> "Ledger"
                            "categories" -> stringResource(R.string.category_label)
                            "analytics" -> stringResource(R.string.analytics_title)
                            "budget" -> stringResource(R.string.budget_title)
                            "goals" -> "Goals"
                            "accounts" -> "Accounts"
                            "settings" -> stringResource(R.string.settings_title)
                            else -> stringResource(R.string.app_name)
                        },
                        showSearch = currentRoute == "dashboard",
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onBackClick = { navController.popBackStack() },
                        onSearchClick = {
                            if (currentRoute != "transactions") {
                                navController.navigate("transactions")
                            }
                        },
                        onAddClick = { 
                            when (currentRoute) {
                                "budget" -> budgetViewModel.triggerAddBudget()
                                "accounts" -> accountsViewModel.triggerAddAccount()
                                "categories" -> categoriesViewModel.triggerAddCategory()
                                "goals" -> goalsViewModel.triggerAddGoal()
                                else -> navController.navigate("add_transaction")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isFullScreenRoute) {
                    ModernNavigationBar(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreenRoute) PaddingValues(0.dp) else innerPadding)
                    .consumeWindowInsets(if (isFullScreenRoute) PaddingValues(0.dp) else innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 2 } },
                exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally { -it / 2 } },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { -it / 2 } },
                popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally { it / 2 } }
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
                        },
                        onNavigateToEditTransaction = { id ->
                            navController.navigate("add_transaction?transactionId=$id")
                        }
                    )
                }
                composable("transactions") {
                    val vm: TransactionViewModel = hiltViewModel()
                    TransactionScreen(
                        viewModel = vm,
                        onNavigateToEdit = { id ->
                            navController.navigate("add_transaction?transactionId=$id")
                        }
                    )
                }
                composable("accounts") {
                    AccountsScreen(accountsViewModel)
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
                    BudgetScreen(budgetViewModel)
                }
                composable("goals") {
                    GoalsScreen(goalsViewModel)
                }
                composable("categories") {
                    CategoriesScreen(categoriesViewModel)
                }
                composable(
                    route = "add_transaction?transactionId={transactionId}",
                    arguments = listOf(
                        navArgument("transactionId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("transactionId")?.takeIf { it != 0L }
                    AddTransactionScreen(
                        transactionId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernTopAppBar(
    title: String,
    showSearch: Boolean = false,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.padding(end = 12.dp).size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showSearch) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
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
        NavigationItem("dashboard", stringResource(R.string.today), Icons.Default.Home, Icons.Outlined.Home),
        NavigationItem("analytics", stringResource(R.string.analytics_title), Icons.Default.BarChart, Icons.Outlined.BarChart),
        NavigationItem("categories", stringResource(R.string.category_label), Icons.Default.Category, Icons.Outlined.Category),
        NavigationItem("budget", stringResource(R.string.budget_title), Icons.Default.AccountBalance, Icons.Outlined.AccountBalance),
        NavigationItem("goals", "Goals", Icons.Default.Flag, Icons.Outlined.Flag),
        NavigationItem("accounts", "Accounts", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                        style = MaterialTheme.typography.labelSmall,
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
