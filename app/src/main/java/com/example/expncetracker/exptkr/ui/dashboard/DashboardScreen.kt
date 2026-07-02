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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import java.math.BigDecimal
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.components.DistributionSection
import com.example.expncetracker.exptkr.ui.components.SpendingTrendSection
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.core.sms.SmsPermissionManager
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    scrollState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
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
                        scrollState = scrollState,
                        summary = state.data.summary,
                        previousSummary = state.data.previousSummary,
                        recent = state.data.recentTransactions,
                        categories = state.data.allCategories,
                        categoriesJson = state.data.allCategoriesJson,
                        recurring = state.data.recurringTransactions,
                        goals = state.data.goals,
                        trends = state.data.trends,
                        currentFilter = currentFilter,
                        pendingTemplates = state.data.pendingConfirmTemplates,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onSeeAllClick = onNavigateToTransactions,
                        onTransactionClick = { txId ->
                            selectedTxForDetail = state.data.recentTransactions.find { it.id == txId }
                        },
                        onFilterChange = onFilterChange,
                        onConfirmRecurring = { id, confirmed -> viewModel.confirmRecurring(id, confirmed) },
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
        val categoriesJson = (uiState as? DashboardUiState.Success)?.data?.allCategoriesJson ?: emptyList()
        ModalBottomSheet(
            onDismissRequest = { selectedTxForDetail = null },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ) {
            TransactionDetailContent(
                transaction = selectedTxForDetail!!,
                categories = categoriesJson,
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
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    summary: FinancialSummary,
    previousSummary: FinancialSummary?,
    recent: List<Transaction>,
    categories: List<CategoryEntity>,
    categoriesJson: List<com.example.expncetracker.exptkr.domain.model.Category> = emptyList(),
    recurring: List<Transaction>,
    goals: List<GoalEntity>,
    trends: List<SpendingTrend>,
    currentFilter: DateFilter,
    pendingTemplates: List<com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity> = emptyList(),
    onNavigateToAddTransaction: () -> Unit,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onFilterChange: (DateFilter) -> Unit,
    onConfirmRecurring: (Long, Boolean) -> Unit,
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
        state = scrollState,
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

        if (pendingTemplates.isNotEmpty()) {
            item(key = "pending_recurring") {
                PendingRecurringSection(
                    templates = pendingTemplates,
                    onConfirm = onConfirmRecurring
                )
            }
        }

        if (trends.isNotEmpty()) {
            item(key = "trend_chart") {
                SpendingTrendSection(
                    trends = trends,
                    previousSummary = previousSummary,
                    currentSummary = summary
                )
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
                    TransactionListItem(
                        transaction = transaction,
                        categories = categoriesJson,
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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
                            categories = categoriesJson,
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

// ─── SpendingTrendSection removed ────────────────────────────

// ─── GoalProgressItem ─────────────────────────────────────────────────────────

@Composable
fun GoalProgressItem(goal: GoalEntity, modifier: Modifier = Modifier) {
    val targetProgress = if (goal.targetAmount.toDouble() > 0)
        (goal.currentAmount.toDouble() / goal.targetAmount.toDouble()).toFloat().coerceIn(0f, 1f)
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
        if (summary.totalAssets.toDouble() > 0) {
            ((summary.netWorth.toDouble() / summary.totalAssets.toDouble()) * 100).toInt().coerceIn(0, 100)
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
                    .padding(bottom = 4.dp)
                    .height(34.dp),
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
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
                        text = summary.spendableBalance.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03A9F4)
                    )

                    Text(
                        text = "Liquid Balance",
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
fun FilterItemCompact(
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
// ─── DistributionSection removed ──────────────────────────────────────────

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

// ─── DistributionItem removed ──────────────────────────────────────────────

@Composable
fun PendingRecurringSection(
    templates: List<com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity>,
    onConfirm: (Long, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SectionHeader(
            title = "Smart detections",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(templates) { template ->
                Card(
                    modifier = Modifier.width(240.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Recurring bill detected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(template.merchantName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Approx. ${template.amount.formatAsCurrency()} monthly", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onConfirm(template.id, false) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Ignore", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = { onConfirm(template.id, true) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Track", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DistributionItem(
    val name: String,
    val amount: java.math.BigDecimal,
    val iconName: String,
    val color: Color
)