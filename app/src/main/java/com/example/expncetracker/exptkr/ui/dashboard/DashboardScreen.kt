package com.example.expncetracker.exptkr.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.R
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Locale

private const val MAX_RECENT_TRANSACTIONS = 15

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToStatementLedger: () -> Unit = {},
    onNavigateToEditTransaction: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val trends by viewModel.trends.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.READ_SMS] == true) {
            viewModel.syncTransactions()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is DashboardUiState.Error -> {
                Text(
                    "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DashboardUiState.Success -> {
                val pullRefreshState = rememberPullToRefreshState()
                
                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = { viewModel.syncTransactions() },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    DashboardContent(
                        summary = state.summary,
                        recent = state.recentTransactions,
                        categories = state.categories,
                        recurring = state.recurringTransactions,
                        goals = state.goals,
                        trends = trends,
                        currentFilter = currentFilter,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onSeeAllClick = onNavigateToStatementLedger,
                        onTransactionClick = onNavigateToEditTransaction,
                        onFilterChange = { viewModel.setFilter(it) },
                        onSyncClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                viewModel.syncTransactions()
                            } else {
                                permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_SMS))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    summary: FinancialSummary,
    recent: List<Transaction>,
    categories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    recurring: List<Transaction>,
    goals: List<com.example.expncetracker.exptkr.data.db.entity.GoalEntity>,
    trends: List<com.example.expncetracker.exptkr.domain.model.SpendingTrend>,
    currentFilter: DateFilter,
    onNavigateToAddTransaction: () -> Unit,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onFilterChange: (DateFilter) -> Unit,
    onSyncClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedContent(
                targetState = recurring.isNotEmpty(),
                label = "greeting"
            ) { hasUpcoming ->
                if (hasUpcoming) {
                    UpcomingPaymentBanner(recurringCount = recurring.size)
                } else {
                    GreetingHeader()
                }
            }
        }

        item {
            NetWorthCard(summary = summary)
        }

        item {
            CompactSummaryHeader(summary = summary, currentFilter = currentFilter, onFilterChange = onFilterChange)
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DistributionSection(
                    distribution = summary.categoryDistribution,
                    allCategories = categories
                )
            }
        }

        if (recurring.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Upcoming Payments",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(recurring, key = { "rec_${it.id}" }) { tx ->
                val categoryEntity = categories.find { it.name == tx.categoryName }
                TransactionListItem(
                    transaction = tx.copy(timestamp = tx.nextDueDate ?: tx.timestamp),
                    categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                    categoryColor = categoryEntity?.let { Color(it.color) },
                    onClick = {} // Read-only on dashboard
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(trends) {
                    if (trends.isNotEmpty()) {
                        modelProducer.runTransaction {
                            lineSeries {
                                series(trends.map { it.amount.toFloat() })
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(rememberLineCartesianLayer()),
                        modelProducer = modelProducer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (goals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Savings Goals",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    goals.forEach { goal ->
                        GoalProgressItem(goal = goal)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent Activity",
                actionLabel = "See All Ledger",
                onActionClick = onSeeAllClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (recent.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No transactions yet",
                        description = "Start by adding a transaction manually or syncing your SMS",
                        action = {
                            Button(
                                onClick = onNavigateToAddTransaction,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add First Transaction")
                            }
                        }
                    )
                }
            }
        } else {
            val limitedRecent = recent.take(MAX_RECENT_TRANSACTIONS)
            val grouped = limitedRecent.groupBy { 
                it.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
            }
            
            grouped.forEach { (date, transactions) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                items(transactions, key = { it.id }) { transaction ->
                    val categoryEntity = categories.find { it.name == transaction.categoryName }
                    TransactionListItem(
                        transaction = transaction,
                        categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                        categoryColor = categoryEntity?.let { Color(it.color) },
                        onClick = null // Disabled editing for recent activity
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun GoalProgressItem(goal: com.example.expncetracker.exptkr.data.db.entity.GoalEntity) {
    val progress = if (goal.targetAmount > 0) {
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    
    Card(
        modifier = Modifier.width(150.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                Canvas(modifier = Modifier.size(50.dp)) {
                    drawArc(
                        color = Color(goal.color).copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(goal.color),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${goal.currentAmount.formatAsCurrency()} / ${goal.targetAmount.formatAsCurrency()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            goal.deadline?.let { deadlineMillis ->
                val deadlineDate = java.time.Instant.ofEpochMilli(deadlineMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                Text(
                    text = "Due: ${deadlineDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun NetWorthCard(summary: FinancialSummary) {
    val isDark = MaterialTheme.isDark
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "NET WORTH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.netWorth.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL ASSETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.totalAssets.formatAsCurrency(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkIncome else LightIncome,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL LIABILITIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.totalLiabilities.formatAsCurrency(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkExpense else LightExpense,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Health Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val healthScore = remember(summary) {
                    if (summary.totalAssets > 0) {
                        ((summary.netWorth / summary.totalAssets) * 100).toInt().coerceIn(0, 100)
                    } else 0
                }
                Text(
                    text = "$healthScore%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        healthScore > 70 -> if (isDark) DarkIncome else LightIncome
                        healthScore > 40 -> MaterialTheme.colorScheme.primary
                        else -> if (isDark) DarkExpense else LightExpense
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSummaryHeader(
    summary: FinancialSummary,
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit
) {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.getDefault()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.isDark

    var showDatePicker by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TimeFilterRow(
                currentFilter = currentFilter,
                onFilterSelected = { onFilterChange(it); showFilterSheet = false }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = yearMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        yearMonth = YearMonth.from(selectedDate)
                        // We must update the filter type to MONTH to ensure the summary updates
                        onFilterChange(DateFilter.MONTH)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    yearMonth = yearMonth.minusMonths(1)
                    onFilterChange(DateFilter.MONTH)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            showDatePicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = yearMonth.format(monthYearFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Row {
                    val isNextMonthInFuture = remember(yearMonth) {
                        yearMonth.plusMonths(1).isAfter(YearMonth.now())
                    }
                    IconButton(
                        onClick = {
                            yearMonth = yearMonth.plusMonths(1)
                            onFilterChange(DateFilter.MONTH)
                        },
                        enabled = !isNextMonthInFuture
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = "Next", 
                            tint = if (isNextMonthInFuture) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryColumn("EXPENSE", summary.totalExpense.formatAsCurrency(), if (isDark) DarkExpense else LightExpense, Modifier.weight(1f))
                SummaryColumn("INCOME", summary.totalIncome.formatAsCurrency(), if (isDark) DarkIncome else LightIncome, Modifier.weight(1f))
                SummaryColumn("TOTAL", summary.balance.formatAsCurrency(),
                    if (summary.balance >=0) (if (isDark) DarkIncome else LightIncome) else
                        (if (isDark) DarkExpense else LightExpense), Modifier.weight(1f))
            }
            
            if (summary.totalLent > 0 || summary.totalBorrowed > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DebtSummaryItem("OWED TO YOU", summary.totalLent.formatAsCurrency(), if (isDark) CategorySalaryDark else CategorySalary)
                    DebtSummaryItem("YOU OWE", summary.totalBorrowed.formatAsCurrency(), if (isDark) CategoryRentDark else CategoryRent)
                }
            }
        }
    }
}

@Composable
fun GreetingHeader() {
    val now = java.time.LocalTime.now()
    val greeting = when (now.hour) {
        in 0..11 -> stringResource(R.string.greeting_morning)
        in 12..15 -> stringResource(R.string.greeting_afternoon)
        in 16..20 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text(
            text = "$greeting!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Welcome back to ${stringResource(R.string.app_name)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UpcomingPaymentBanner(recurringCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationImportant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Upcoming Payment Due!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "You have $recurringCount bill${if (recurringCount > 1) "s" else ""} to pay soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                 scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "SummaryAnimation"
        ) { targetValue ->
            Text(
                text = targetValue,
                style = if (targetValue.length > 12) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun DebtSummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

private data class DistributionItem(
    val name: String,
    val amount: Double,
    val category: Category,
    val color: Color
)

@Composable
fun DistributionSection(
    distribution: Map<String, Double>,
    allCategories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>
) {
    val distributionData = remember(distribution, allCategories) {
        allCategories.map { entity ->
            val amount = distribution[entity.name] ?: 0.0
            val categoryEnum = Category.entries.find { it.displayName == entity.name } ?: Category.OTHERS
            DistributionItem(entity.name, amount, categoryEnum, Color(entity.color))
        }.sortedByDescending { it.amount }
    }
    
    val maxVal = remember(distributionData) {
        distributionData.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column {
        Text(
            text = "Spending Distribution",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            distributionData.forEach { item ->
                val barHeightFraction = (item.amount / maxVal).toFloat().coerceIn(0.01f, 1f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(44.dp)
                ) {
                    Text(
                        text = if (item.amount >= 1000) String.format(Locale.US, "%.1fK", item.amount/1000) else item.amount.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(item.color.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = item.color.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFraction)
                                .background(item.color)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun getCategoryColor(category: Category, isDarkTheme: Boolean): Color {
    return when (category) {
        Category.FOOD -> if (isDarkTheme) CategoryFoodDark else CategoryFood
        Category.CABS -> if (isDarkTheme) CategoryCabsDark else CategoryCabs
        Category.RENT -> if (isDarkTheme) CategoryRentDark else CategoryRent
        Category.BILLS -> if (isDarkTheme) CategoryBillsDark else CategoryBills
        Category.SHOPPING -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.SALARY -> if (isDarkTheme) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS -> if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL -> if (isDarkTheme) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES -> if (isDarkTheme) CategoryGroceriesDark else CategoryGroceries
        Category.HEALTHCARE -> if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}

@Composable
fun TimeFilterRow(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    Column {
        Text(
            text = "Select Period",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DateFilter.entries.forEach { filter ->
                val isSelected = currentFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = if (!isSelected) {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            borderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    } else null
                )
            }
        }
    }
}
