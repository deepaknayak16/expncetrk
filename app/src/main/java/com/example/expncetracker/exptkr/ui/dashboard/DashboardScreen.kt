package com.example.expncetracker.exptkr.ui.dashboard

import android.Manifest
import android.R
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.animation.core.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionDetailContent
import java.time.LocalDateTime
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

private const val MAX_RECENT_TRANSACTIONS = 25

// ─── DashboardScreen ──────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToEditTransaction: (Long) -> Unit = {},
    onFilterChange: (DateFilter) -> Unit = { viewModel.setFilter(it) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current
    
    var selectedTxForDetail by remember { mutableStateOf<Transaction?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true && permissions[Manifest.permission.RECEIVE_SMS] == true) {
            viewModel.syncTransactions()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            is DashboardUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(26.dp))
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { viewModel.syncTransactions() }) {
                        Text("Retry")
                    }
                }
            }
            is DashboardUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = { viewModel.syncTransactions() },
                    state = rememberPullToRefreshState(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    DashboardContent(
                        viewModel = viewModel,
                        summary = state.data.summary,
                        recent = state.data.recentTransactions,
                        categories = state.data.allCategories,
                        recurring = state.data.recurringTransactions,
                        goals = state.data.goals,
                        trends = state.data.trends,
                        currentFilter = currentFilter,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onSeeAllClick = onNavigateToTransactions,
                        onTransactionClick = { txId ->
                            selectedTxForDetail = state.data.recentTransactions.find { it.id == txId }
                        },
                        onFilterChange = onFilterChange,
                        onSyncClick = {
                            if (SmsPermissionManager.hasPermissions(context)) {
                                viewModel.syncTransactions()
                            } else {
                                permissionLauncher.launch(SmsPermissionManager.permissions)
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    if (selectedTxForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTxForDetail = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TransactionDetailContent(
                transaction = selectedTxForDetail!!,
                onEdit = {
                    onNavigateToEditTransaction(selectedTxForDetail!!.id)
                    selectedTxForDetail = null
                },
                onDelete = {
                    viewModel.deleteTransaction(selectedTxForDetail!!)
                    selectedTxForDetail = null
                },
                onSplit = {
                    selectedTxForDetail = null
                },
                onSettle = {
                    viewModel.settleTransaction(selectedTxForDetail!!)
                    selectedTxForDetail = null
                }
            )
        }
    }
}

// ─── DashboardContent ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    viewModel: DashboardViewModel,
    summary: FinancialSummary,
    recent: List<Transaction>,
    categories: List<CategoryEntity>,
    recurring: List<Transaction>,
    goals: List<GoalEntity>,
    trends: List<SpendingTrend>,
    currentFilter: DateFilter,
    onNavigateToAddTransaction: () -> Unit,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onFilterChange: (DateFilter) -> Unit,
    onSyncClick: () -> Unit,
) {

    var showUpcomingSheet by remember { mutableStateOf(false) }

    val groupedRecent = remember(recent) {
        recent.take(MAX_RECENT_TRANSACTIONS)
            .groupBy { tx ->
                tx.timestamp.format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
                )
            }
    }

    val upcomingRecurring = remember(recurring) {
        val today = LocalDate.now()
        recurring.filter { tx ->
            tx.nextDueDate?.let { due ->
                val dueDate = due.toLocalDate()
                // Include overdue and anything due in the next 7 days
                ChronoUnit.DAYS.between(today, dueDate) <= 7
            } ?: false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            ModernDashboardHeader(
                viewModel = viewModel,
                summary = summary,
                recurringTransactions = recurring,
                currentFilter = currentFilter,
                onFilterChange = onFilterChange,
                onUpcomingClick = { showUpcomingSheet = true },
                onSyncClick = onSyncClick
            )
        }

        if (trends.isNotEmpty()) {
            item(key = "trend_chart") {
                SpendingTrendSection(trends = trends)
            }
        }

        item(key = "distribution") {
            DistributionSection(
                distribution = summary.categoryDistribution,
                allCategories = categories,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (goals.isNotEmpty()) {
            item(key = "goals_header") {
                SectionHeader(
                    title = "Savings goals",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item(key = "goals_row") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(goals) { goal ->
                        GoalProgressItem(goal = goal, modifier = Modifier.width(160.dp))
                    }
                }
            }
        }

        item(key = "recent_header") {
            SectionHeader(
                title = "Recent activity",
                style = MaterialTheme.typography.titleMedium,
                actionLabel = "See all",
                onActionClick = onSeeAllClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (recent.isEmpty()) {
            item(key = "empty_state") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions yet",
                    description = "Add a transaction manually or sync your SMS",
                    action = {
                        Button(
                            onClick = onNavigateToAddTransaction,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add first transaction")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            groupedRecent.forEach { (date, transactions) ->
                item(key = "date_$date") {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(transactions, key = { "tx_${it.id}" }) { transaction ->
                    val categoryEntity = categories.find { it.name == transaction.categoryName }
                    TransactionListItem(
                        transaction = transaction,
                        categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                        categoryColor = categoryEntity?.let { Color(it.color) },
                        onClick = { onTransactionClick(transaction.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showUpcomingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUpcomingSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Upcoming Payments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(6.dp)
                )

                if (upcomingRecurring.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No upcoming payments in the next 7 days")
                    }
                } else {
                    upcomingRecurring.forEach { tx ->
                        val categoryEntity = categories.find { it.name == tx.categoryName }
                        TransactionListItem(
                            transaction = tx.copy(timestamp = tx.nextDueDate ?: tx.timestamp),
                            categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                            categoryColor = categoryEntity?.let { Color(it.color) },
                            onClick = {
                                onTransactionClick(tx.id)
                                showUpcomingSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── SpendingTrendSection (REDESIGNED - Area Chart with Gradient) ────────────

@Composable
private fun SpendingTrendSection(trends: List<SpendingTrend>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val totalSpending = remember(trends) { trends.sumOf { it.amount } }
    val percentageChange = remember(trends) {
        if (trends.size >= 2) {
            val first = trends.first().amount
            val last = trends.last().amount
            if (first > 0) ((last - first) / first * 100).toInt() else 0
        } else 0
    }

    LaunchedEffect(trends) {
        modelProducer.runTransaction {
            lineSeries { series(trends.map { it.amount.toFloat() }) }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalSpending.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (percentageChange >= 0)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "${if (percentageChange >= 0) "+" else ""}$percentageChange%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (percentageChange >= 0)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val line = rememberLine(
                fill = LineCartesianLayer.LineFill.single(fill(MaterialTheme.colorScheme.primary)),
                thickness = 3.dp,
                areaFill = LineCartesianLayer.AreaFill.single(
                    fill(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(line)
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ─── GoalProgressItem ─────────────────────────────────────────────────────────

@Composable
fun GoalProgressItem(goal: GoalEntity, modifier: Modifier = Modifier) {
    val targetProgress = if (goal.targetAmount > 0)
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val goalColor = Color(goal.color)
            val onSurface = MaterialTheme.colorScheme.onSurface
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    drawArc(
                        color = surfaceVariant,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = goalColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(targetProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = goal.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = goal.currentAmount.formatAsCurrency(),
                style = MaterialTheme.typography.labelMedium,
                color = goalColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            goal.deadline?.let { deadlineMillis ->
                val deadlineDate = Instant.ofEpochMilli(deadlineMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                Text(
                    text = "Due ${deadlineDate.format(DateTimeFormatter.ofPattern("MMM yy"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

// ─── ModernDashboardHeader ────────────────────────────────────────────────────
fun getPaymentStatus(
    transactions: List<Transaction>
): PaymentStatus {

    val today = LocalDate.now()

    var dueSoon = false
    var dueToday = false

    transactions.forEach { transaction ->

        val dueDate = transaction.nextDueDate ?: return@forEach

        val days =
            ChronoUnit.DAYS
                .between(today, dueDate.toLocalDate())

        when {
            days <= 0L -> {
                dueToday = true
            }

            days <= 2L -> {
                dueSoon = true
            }
        }
    }

    return when {
        dueToday -> PaymentStatus.TODAY
        dueSoon -> PaymentStatus.SOON
        else -> PaymentStatus.NORMAL
    }
}
enum class PaymentStatus {
    NORMAL,
    SOON,
    TODAY
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDashboardHeader(
    viewModel: DashboardViewModel,
    summary: FinancialSummary,
    recurringTransactions: List<Transaction>,
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    onUpcomingClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val healthScore = remember(summary) {
        if (summary.totalAssets > 0) {
            ((summary.netWorth / summary.totalAssets) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    val today = LocalDate.now()
    val upcomingTransactions = remember(recurringTransactions) {
        recurringTransactions.filter { tx ->
            tx.nextDueDate?.let { due ->
                val dueDate = due.toLocalDate()
                !dueDate.isBefore(today) && ChronoUnit.DAYS.between(today, dueDate) <= 7
            } ?: false
        }
    }

    val upcomingCount = upcomingTransactions.size
    val paymentStatus = remember(recurringTransactions) { getPaymentStatus(recurringTransactions) }
    val dominantType = remember(upcomingTransactions) {
        if (upcomingTransactions.isEmpty()) TransactionType.DEBIT
        else {
            val todayTxs = upcomingTransactions.filter { it.nextDueDate?.toLocalDate() == LocalDate.now() }
            val targetList = if (todayTxs.isNotEmpty()) todayTxs else upcomingTransactions
            targetList.groupBy { it.type }.maxByOrNull { it.value.size }?.key ?: TransactionType.DEBIT
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (upcomingCount > 0) {
            val isToday = paymentStatus == PaymentStatus.TODAY
            val infiniteTransition = rememberInfiniteTransition(label = "paymentBlink")
            val flashAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isToday) 0.6f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "flash"
            )

            val isIncomeType =
                dominantType == TransactionType.CREDIT || dominantType == TransactionType.BORROW
            val containerColor = when (paymentStatus) {
                PaymentStatus.TODAY ->
                    if (isIncomeType) Color(0xFFE8F5E9).copy(alpha = flashAlpha)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = flashAlpha)

                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val contentColor = when (paymentStatus) {
                PaymentStatus.TODAY ->
                    if (isIncomeType) Color(0xFF2E7D32)
                    else MaterialTheme.colorScheme.error

                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = onUpcomingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isToday) Icons.Default.PriorityHigh else Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (paymentStatus) {
                            PaymentStatus.TODAY -> "$upcomingCount Payments Due Today"
                            else -> "$upcomingCount Payments Due Soon"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }

        // Date selector
        Surface(
            modifier = Modifier.fillMaxWidth()
            .padding(top = 2.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 12.dp),
                    //.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterItemCompact(
                    DateFilter.DAY,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(1f)
                )
                FilterItemCompact(
                    DateFilter.WEEK,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(1f)
                )

                // Month/Year display & controls
                Row(
                    modifier = Modifier.weight(2.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val currentViewDate by viewModel.selectedDate.collectAsState()
                    IconButton(
                        onClick = {
                            val nextDate = when (currentFilter) {
                                DateFilter.YEAR -> currentViewDate.minusYears(1)
                                else -> currentViewDate.minusMonths(1)
                            }
                            viewModel.updateDate(nextDate)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Previous", modifier = Modifier.size(20.dp))
                    }

                    Text(
                        text = if (currentFilter == DateFilter.YEAR) currentViewDate.year.toString()
                        else currentViewDate.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    val isNextMonthInFuture = remember(currentViewDate, currentFilter) {
                        val next = when (currentFilter) {
                            DateFilter.YEAR -> currentViewDate.plusYears(1)
                            else -> currentViewDate.plusMonths(1)
                        }
                        next.isAfter(LocalDateTime.now())
                    }

                    IconButton(
                        onClick = {
                            val nextDate = when (currentFilter) {
                                DateFilter.YEAR -> currentViewDate.plusYears(1)
                                else -> currentViewDate.plusMonths(1)
                            }
                            viewModel.updateDate(nextDate)
                        },
                        enabled = !isNextMonthInFuture,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, "Next", modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.weight(1f).size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                FilterItemCompact(
                    DateFilter.MONTH,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(1f)
                )
                FilterItemCompact(
                    DateFilter.YEAR,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(1.dp))

        val healthColor =
            when {
                healthScore > 70 -> Color(0xFF2E7D32)
                healthScore > 40 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }

        val animatedHealth by animateFloatAsState(
            targetValue = healthScore / 100f,
            animationSpec = tween(1000),
            label = "healthScore"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {

            // ───────── LEFT CARD ─────────
            SummaryCard(modifier = Modifier.weight(1f)) {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    CardSectionLabel("Cash flow")

                    Text(
                        text = summary.balance.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03A9F4)
                    )

                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompactRow(
                        label = "Income",
                        value = summary.totalIncome.formatAsCurrency(),
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelLarge
                    )

                    CompactRow(
                        label = "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        value = summary.totalExpense.formatAsCurrency(),
                        color = MaterialTheme.colorScheme.error
                    )

                    CardDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LendBorrowItem(
                            label = "Owe you",
                            value = summary.totalLent.formatAsCurrency(),
                            amountColor = Color(0xFF673AB7)
                        )

                        LendBorrowItem(
                            label = "You owe",
                            value = summary.totalBorrowed.formatAsCurrency(),
                            amountColor = Color(0xFFFF9800),
                            alignEnd = true
                        )
                    }
                }
            }

            // ───────── RIGHT CARD ─────────
            SummaryCard(modifier = Modifier.weight(1f)) {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    CardSectionLabel("Portfolio")

                    Text(
                        text = summary.netWorth.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03A9F4)
                    )

                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CompactRow("Assets", style = MaterialTheme.typography.labelLarge, value = summary.totalAssets.formatAsCurrency(),color = Color(0xFF2E7D32))
                    CompactRow("Liabilities", style = MaterialTheme.typography.labelLarge, value = summary.totalLiabilities.formatAsCurrency(), color = MaterialTheme.colorScheme.error)

                    CardDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "$healthScore%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = healthColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedHealth },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(CircleShape),
                        color = healthColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterItemCompact(
    filter: DateFilter,
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentFilter == filter

    Surface(
        onClick = { onFilterChange(filter) },
        modifier = modifier.height(24.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = filter.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Header sub-components ────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun CardSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun CompactRow(
    label: String,
    value: String,
    color: Color = LocalContentColor.current,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = style,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            value,
            style = style,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun RowScope.LendBorrowItem(
    label: String,
    value: String,
    alignEnd: Boolean = false,
    amountColor: Color
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor   // ✅ HERE
        )
    }
}
@Composable
fun DistributionSection(
    distribution: Map<String, Double>,
    allCategories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    showCard: Boolean = true,
    onCategoryClick: ((String) -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.isDark
    val distributionData = remember(
        distribution,
        allCategories,
        isDarkTheme
    ) {
        allCategories.mapNotNull { entity ->

            val amount = distribution[entity.name]
                ?: return@mapNotNull null

            if (amount <= 0.0) return@mapNotNull null

            val categoryEnum =
                Category.entries.find {
                    it.displayName == entity.name
                } ?: Category.OTHERS

            DistributionItem(
                name = entity.name,
                amount = amount,
                category = categoryEnum,
                color = getCategoryColor(
                    categoryEnum,
                    isDarkTheme
                )
            )
        }.sortedByDescending { it.amount }
    }

    if (distributionData.isEmpty()) return

    val maxAmount = remember(distributionData) {
        distributionData.maxOf { it.amount }
            .coerceAtLeast(1.0)
    }

    Column(modifier = modifier) {

        if (showTitle) {
            Text(
                text = "Spending Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))
        }

        if (showCard) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                DistributionList(
                    distributionData = distributionData,
                    maxAmount = maxAmount,
                    onCategoryClick = onCategoryClick
                )
            }
        } else {
            DistributionList(
                distributionData = distributionData,
                maxAmount = maxAmount,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun DistributionList(
    distributionData: List<DistributionItem>,
    maxAmount: Double,
    onCategoryClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        distributionData.forEach { item ->

            val progress =
                (item.amount / maxAmount)
                    .toFloat()
                    .coerceIn(0f, 1f)

            // Guarantees text visibility
            val minBarWidth = 180.dp
            val extraWidth = 180.dp

            val barWidth =
                minBarWidth + (extraWidth * progress)

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = onCategoryClick != null) {
                        onCategoryClick?.invoke(item.name)
                    }
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.15f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = MaterialTheme.shapes.small
                    )
            ) {

                // Main liquid color layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    item.color.copy(alpha = 0.95f),
                                    item.color.copy(alpha = 0.75f),
                                    item.color.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Glass highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f)
                                )
                            )
                        )
                )

                // Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = item.amount.formatAsCurrency(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── TimeFilterRow ────────────────────────────────────────────────────────────

@Composable
fun TimeFilterRow(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select period",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DateFilter.entries.forEach { filter ->
                    val isSelected = currentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter.title) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

// ─── getCategoryColor ─────────────────────────────────────────────────────────

fun getCategoryColor(category: Category, isDarkTheme: Boolean): Color {
    return when (category) {
        Category.FOOD          -> if (isDarkTheme) CategoryFoodDark else CategoryFood
        Category.CABS          -> if (isDarkTheme) CategoryCabsDark else CategoryCabs
        Category.RENT          -> if (isDarkTheme) CategoryRentDark else CategoryRent
        Category.BILLS         -> if (isDarkTheme) CategoryBillsDark else CategoryBills
        Category.SHOPPING      -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.SALARY        -> if (isDarkTheme) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS   -> if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL        -> if (isDarkTheme) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES     -> if (isDarkTheme) CategoryGroceriesDark else CategoryGroceries
        Category.HEALTHCARE    -> if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION     -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.OTHERS        -> if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}

@Composable
fun DashboardInfoItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class DistributionItem(
    val name: String,
    val amount: Double,
    val category: Category,
    val color: Color
)