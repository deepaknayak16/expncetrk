package com.example.expncetracker.exptkr.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.expncetracker.exptkr.R
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.ui.accounts.AccountsScreen
import com.example.expncetracker.exptkr.ui.accounts.AccountsViewModel
import com.example.expncetracker.exptkr.ui.addtransaction.AddTransactionScreen
import com.example.expncetracker.exptkr.ui.analytics.AnalyticsScreen
import com.example.expncetracker.exptkr.ui.bills.BillsScreen
import com.example.expncetracker.exptkr.ui.bills.BillsViewModel
import com.example.expncetracker.exptkr.ui.budget.BudgetScreen
import com.example.expncetracker.exptkr.ui.budget.BudgetViewModel
import com.example.expncetracker.exptkr.ui.categories.CategoriesScreen
import com.example.expncetracker.exptkr.ui.categories.CategoriesViewModel
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.ui.dashboard.DashboardScreen
import com.example.expncetracker.exptkr.ui.dashboard.DashboardUiState
import com.example.expncetracker.exptkr.ui.dashboard.DashboardViewModel
import com.example.expncetracker.exptkr.ui.goals.GoalsScreen
import com.example.expncetracker.exptkr.ui.goals.GoalsViewModel
import com.example.expncetracker.exptkr.ui.settings.SettingsScreen
import com.example.expncetracker.exptkr.ui.settings.SettingsViewModel
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.ui.transactions.TransactionScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ─── Nav items ────────────────────────────────────────────────────────────────

data class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    NavigationItem("dashboard", "Today", Icons.Filled.Home, Icons.Outlined.Home),
    NavigationItem("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavigationItem("budget", "Budget", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    NavigationItem("categories", "Category", Icons.Filled.Category, Icons.Outlined.Category),
    NavigationItem("goals", "Goals", Icons.Filled.Flag, Icons.Outlined.Flag),
    NavigationItem("accounts", "Accounts", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
)

// ─── AppNavGraph ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(startRoute: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Handle deep-links from startRoute
    LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank()) {
            navController.navigate(startRoute) {
                // Ensure we don't pile up routes if multiple deep links are clicked
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val activity = remember(context) { context.findActivity() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ── Scroll States ──────────────────────────────────────────────────────────
    //val dashboardScrollState = rememberLazyListState()

    // ── ViewModels ─────────────────────────────────────────────────────────────
    val settingsViewModel: SettingsViewModel = if (activity != null)
        hiltViewModel(activity) else hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val accountsViewModel: AccountsViewModel = hiltViewModel()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val goalsViewModel: GoalsViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val billsViewModel: BillsViewModel = hiltViewModel()

    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    // ── Sheet / overlay state ──────────────────────────────────────────────────
    var showUpcomingSheet by remember { mutableStateOf(false) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var editingTransactionId by remember { mutableStateOf<Long?>(null) }

    // FIX: filter recurring — only next 7 days, derived from dashboard state
    val upcomingRecurring = remember(dashboardState) {
        (dashboardState as? DashboardUiState.Success)
            ?.data?.recurringTransactions
            ?.filter { tx ->
                tx.nextDueDate?.let { due ->
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), due.toLocalDate())
                    days in 0..7
                } ?: false
            } ?: emptyList()
    }

    // FIX: full-screen routes — no top/bottom bar, no drawer gestures
    val isFullScreen = currentRoute?.startsWith("add_transaction") == true

    // NEW: Calculate if any payment is due specifically TODAY
    val isPaymentDueToday = upcomingRecurring.any {
        it.nextDueDate?.toLocalDate() == LocalDate.now()
    }

    // ── Drawer ─────────────────────────────────────────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isFullScreen,
        drawerContent = {
            AppDrawerSheet(
                currentRoute = currentRoute,
                isSignedIn = settingsUiState.isSignedIn,
                userName = settingsUiState.accountName,
                userPhotoUrl = settingsUiState.accountPhotoUrl,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!isFullScreen) {
                    AppTopBar(
                        currentRoute = currentRoute,
                        isSignedIn = settingsUiState.isSignedIn,
                        userName = settingsUiState.accountName,
                        userPhotoUrl = settingsUiState.accountPhotoUrl,
                        hasUpcomingPayments = upcomingRecurring.isNotEmpty(),
                        isPaymentDueToday = isPaymentDueToday,
                        upcomingCount = upcomingRecurring.size,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNotificationClick = { showUpcomingSheet = true },
                        onSearchClick = {
                            if (currentRoute != "transactions")
                                navController.navigate("transactions")
                        },
                        onAddClick = {
                            when (currentRoute) {
                                "budget" -> budgetViewModel.triggerAddBudget()
                                "accounts" -> accountsViewModel.triggerAddAccount()
                                "categories" -> categoriesViewModel.triggerAddCategory()
                                "goals" -> goalsViewModel.triggerAddGoal()
                                else -> {
                                    navController.navigate("add_transaction")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isFullScreen) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
                    .consumeWindowInsets(if (isFullScreen) PaddingValues(0.dp) else innerPadding),
                enterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally { it / 3 }
                },
                exitTransition = {
                    fadeOut(tween(220)) + slideOutHorizontally { -it / 3 }
                },
                popEnterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally { -it / 3 }
                },
                popExitTransition = {
                    fadeOut(tween(220)) + slideOutHorizontally { it / 3 }
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        //scrollState = dashboardScrollState,
                        onNavigateToAddTransaction = {
                            navController.navigate("add_transaction")
                        },
                        onNavigateToTransactions = { navController.navigate("transactions") },
                        onNavigateToAnalytics = { navController.navigate("analytics") },
                        onNavigateToEditTransaction = { id ->
                            navController.navigate("add_transaction?transactionId=$id")
                        },
                        onFilterChange = { dashboardViewModel.setFilter(it) }
                    )
                }
                composable("transactions") {
                    TransactionScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToEdit = { id ->
                            navController.navigate("add_transaction?transactionId=$id")
                        }
                    )
                }
                composable("analytics") {
                    AnalyticsScreen(viewModel = hiltViewModel())
                }
                composable("bills") {
                    BillsScreen(billsViewModel)
                }
                composable("budget") {
                    BudgetScreen(budgetViewModel)
                }
                composable("goals") {
                    GoalsScreen(goalsViewModel)
                }
                composable("accounts") {
                    AccountsScreen(accountsViewModel)
                }
                composable("categories") {
                    CategoriesScreen(categoriesViewModel)
                }
                composable("settings") {
                    SettingsScreen(settingsViewModel)
                }
                composable(
                    route = "add_transaction?transactionId={transactionId}",
                    arguments = listOf(
                        navArgument("transactionId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { backStack ->
                    val id = backStack.arguments?.getLong("transactionId")?.takeIf { it != 0L }
                    AddTransactionScreen(
                        transactionId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // ── Upcoming payments sheet ────────────────────────────────────────────────
    if (showUpcomingSheet) {
        val categories = (dashboardState as? DashboardUiState.Success)
            ?.data?.allCategories ?: emptyList()
        val domainCategories: List<Category> = categories.map { entity ->
            Category(
                id = entity.name,
                name = entity.name,
                type = entity.type,
                icon = entity.iconName,
                color = String.format("#%06X", (0xFFFFFF and entity.color))
            )
        }

        ModalBottomSheet(
            onDismissRequest = { showUpcomingSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Text(
                text = "Upcoming payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            if (upcomingRecurring.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No payments due in the next 7 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                upcomingRecurring.forEach { tx ->
                    TransactionListItem(
                        transaction = tx.copy(timestamp = tx.nextDueDate ?: tx.timestamp),
                        categories = domainCategories,
                        onClick = {
                            navController.navigate("add_transaction?transactionId=${tx.id}")
                            showUpcomingSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Add / edit transaction sheet ───────────────────────────────────────────
    if (showAddTransactionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddTransactionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddTransactionScreen(
                transactionId = editingTransactionId,
                onNavigateBack = { showAddTransactionSheet = false }
            )
        }
    }
}

// ─── Drawer sheet ─────────────────────────────────────────────────────────────

@Composable
private fun AppDrawerSheet(
    currentRoute: String?,
    isSignedIn: Boolean,
    userName: String?,
    userPhotoUrl: String?,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet (modifier = Modifier
        .width(260.dp)        // Drawer width
        .fillMaxHeight()      // Full height
        .padding(0.dp)        // Outer padding
    ) {
        // FIX: flat surface header — no gradient (broken in dark mode)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    if (isSignedIn && userPhotoUrl != null) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isSignedIn) Icons.Default.AccountCircle
                            else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (isSignedIn) userName ?: "User"
                    else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isSignedIn) {
                    Text(
                        text = "Signed in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val drawerItems = listOf(
            NavigationItem("dashboard", "Today", Icons.Filled.Home, Icons.Outlined.Home),
            NavigationItem("transactions", "Ledger", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
            NavigationItem("bills", "Bills", Icons.Filled.Receipt, Icons.Outlined.Receipt),
            NavigationItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        drawerItems.forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium)
                },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

// ─── Top app bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    currentRoute: String?,
    isSignedIn: Boolean,
    userName: String?,
    userPhotoUrl: String?,
    hasUpcomingPayments: Boolean,
    isPaymentDueToday: Boolean,
    upcomingCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit
) {
    // 1. Fixed Height
    val appBarHeight = 36.dp
    val paddingSide = 6.dp

    // Calculate Dynamic Screen Title
    val screenTitle = remember(currentRoute, userName, isSignedIn) {
        when (currentRoute) {
            "dashboard" -> {
                val greetingPrefix = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                    in 0..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    in 17..20 -> "Good evening"
                    else -> "Good night"
                }
                if (isSignedIn && !userName.isNullOrBlank()) "$greetingPrefix, $userName" else greetingPrefix
            }
            "transactions" -> "Ledger"
            "analytics" -> "Analytics"
            "bills" -> "Bills"
            "budget" -> "Budget"
            "goals" -> "Goals"
            "accounts" -> "Accounts"
            "categories" -> "Categories"
            "settings" -> "Settings"
            else -> "ExpnceTkr"
        }
    }

    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulse")

    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_rotation"
    )

    // EMOJI STATUS - Placed at the very top (Status Bar / Camera area)
    val backgroundEmoji = when {
        isPaymentDueToday -> "🚨 $upcomingCount Due Today 🚨"
        hasUpcomingPayments -> " UpComing 🕒"
        else -> "$greeting 💰"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // EMOJI STATUS - Placed at the very top (Status Bar / Camera area)
        Text(
            text = backgroundEmoji,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp), // Positioned in the status bar area
            color = if (isPaymentDueToday) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Header container with fixed height
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // Dynamically clear the status bar
                .height(appBarHeight),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingSide),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: App Icon & Titles
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onMenuClick,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = "Open Drawer",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.5f)
                        )
                    }

                    Column {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentRoute == "dashboard" && isPaymentDueToday) {
                            Text(
                                text = "⚠️ $upcomingCount Due Today",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                }

                // Right side: Action icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        onClick = onAddClick,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (hasUpcomingPayments) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (isPaymentDueToday) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp).graphicsLayer {
                                rotationZ = if (hasUpcomingPayments) bellRotation else 0f
                            }
                        )
                    }

                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        if (isSignedIn && userPhotoUrl != null) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─── Bottom navigation bar ────────────────────────────────────────────────────

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    NavigationBar(
        modifier = Modifier
            .height(80.dp)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx()
                )
            },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(
                        HapticFeedbackType.LongPress
                    )
                    onNavigate(item.route)
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon
                        else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 1
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

// ─── Utility ──────────────────────────────────────────────────────────────────

fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
