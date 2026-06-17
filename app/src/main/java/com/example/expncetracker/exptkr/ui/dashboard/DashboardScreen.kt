package com.example.expncetracker.exptkr.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
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

private const val MAX_RECENT_TRANSACTIONS = 25

// ─── DashboardScreen ──────────────────────────────────────────────────────────

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
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
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
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.READ_SMS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.syncTransactions()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_SMS)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── DashboardContent ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
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
                !dueDate.isBefore(today) && ChronoUnit.DAYS.between(today, dueDate) <= 7
            } ?: false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "header") {
            ModernDashboardHeader(
                userName = "Janak",
                summary = summary,
                recurringTransactions = recurring,
                currentFilter = currentFilter,
                onFilterChange = onFilterChange,
                onUpcomingClick = { showUpcomingSheet = true }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(goals) { goal ->
                        GoalProgressItem(goal = goal, modifier = Modifier.width(140.dp))
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
                        fontWeight = FontWeight.SemiBold,
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
                        onClick = null,
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
                            fontWeight = FontWeight.Medium,
                            color = if (percentageChange >= 0)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

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
                    .height(140.dp)
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
    val progress = if (goal.targetAmount > 0)
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    else 0f

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val goalColor = Color(goal.color)
            val onSurface = MaterialTheme.colorScheme.onSurface
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val strokeWidth = 6.dp.toPx()
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
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = goal.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = goal.currentAmount.formatAsCurrency(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    userName: String,
    summary: FinancialSummary,
    recurringTransactions: List<Transaction>,
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit,
    onUpcomingClick: () -> Unit
) {
    val now = LocalTime.now()
    val greeting = when (now.hour) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        in 16..20 -> "Good Evening"
        else -> "Good Night"
    }

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }

    var showRangePicker by remember { mutableStateOf(false) }

    val healthScore = remember(summary) {
        if (summary.totalAssets > 0) {
            ((summary.netWorth / summary.totalAssets) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    val today = LocalDate.now()
    val upcomingCount = remember(recurringTransactions) {
        recurringTransactions.filter { tx ->
            tx.nextDueDate?.let { due ->
                val dueDate = due.toLocalDate()
                !dueDate.isBefore(today) && ChronoUnit.DAYS.between(today, dueDate) <= 7
            } ?: false
        }.size
    }
    
    val paymentStatus = remember(recurringTransactions) {
        getPaymentStatus(recurringTransactions)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "$greeting, $userName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (upcomingCount > 0) {

                val isToday =
                    paymentStatus == PaymentStatus.TODAY


                val infiniteTransition =
                    rememberInfiniteTransition(
                        label = "paymentBlink"
                    )


                val flashAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isToday) 0.45f else 1f,

                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(
                                durationMillis = 700
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),

                    label = "flash"
                )


                val containerColor =
                    when (paymentStatus) {

                        PaymentStatus.TODAY ->
                            MaterialTheme.colorScheme.errorContainer
                                .copy(alpha = flashAlpha)


                        PaymentStatus.SOON ->
                            Color(0xFFFFE4E6)


                        else ->
                            MaterialTheme.colorScheme.surfaceVariant
                    }


                val contentColor =
                    when (paymentStatus) {
                        PaymentStatus.TODAY ->
                            MaterialTheme.colorScheme.error
                        PaymentStatus.SOON ->
                            Color(0xFFDC2626)
                        PaymentStatus.NORMAL ->
                            MaterialTheme.colorScheme.onSurface
                    }



                Spacer(
                    Modifier.width(8.dp)
                )


                AssistChip(

                    onClick = onUpcomingClick,

                    modifier = Modifier
                        .height(30.dp),


                    label = {

                        Text(
                            text =
                                when(paymentStatus){

                                    PaymentStatus.TODAY ->
                                        "$upcomingCount Due Today"

                                    else ->
                                        "$upcomingCount Due"
                                },

                            style =
                                MaterialTheme.typography.labelSmall,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    },


                    leadingIcon = {

                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        contentColor
                                    )
                        )
                    },


                    colors =
                        AssistChipDefaults.assistChipColors(

                            containerColor =
                                containerColor,

                            labelColor =
                                contentColor,

                            leadingIconContentColor =
                                contentColor
                        )
                )
            }
            }

        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilterItemCompact(
                    DateFilter.DAY,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(.7f)
                )

                Spacer(Modifier.width(2.dp))

                FilterItemCompact(
                    DateFilter.WEEK,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(.8f)
                )

                Spacer(Modifier.width(4.dp))

                Row(
                    modifier = Modifier.weight(2f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    IconButton(
                        onClick = {
                            yearMonth = yearMonth.minusMonths(1)
                            onFilterChange(DateFilter.MONTH)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = yearMonth.format(
                            DateTimeFormatter.ofPattern(
                                "MMM yyyy"
                            )
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                showRangePicker = true
                            }
                            .padding(horizontal = 4.dp)
                    )

                    val isNextMonthInFuture = remember(yearMonth) {
                        yearMonth.plusMonths(1).isAfter(YearMonth.now())
                    }

                    IconButton(
                        onClick = {
                            yearMonth = yearMonth.plusMonths(1)
                            onFilterChange(DateFilter.MONTH)
                        },
                        enabled = !isNextMonthInFuture,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                FilterItemCompact(
                    DateFilter.MONTH,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(.8f)
                )

                Spacer(Modifier.width(2.dp))

                FilterItemCompact(
                    DateFilter.YEAR,
                    currentFilter,
                    onFilterChange,
                    Modifier.weight(.7f)
                )
            }
        }

        if (showRangePicker) {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showRangePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = { Text("Select Range", modifier = Modifier.padding(6.dp)) },
                    headline = { 
                        Text(
                            text = "Custom Period", 
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
                    showModeToggle = false
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryCard(modifier = Modifier.weight(1f)) {
                CardSectionLabel("Cash flow" )
                Spacer(Modifier.height(8.dp))
                CompactRow("Balance", summary.balance.formatAsCurrency(), FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                CompactRow("Income", summary.totalIncome.formatAsCurrency(), FontWeight.Bold)

                Spacer(Modifier.height(4.dp))
                CompactRow("Expense", summary.totalExpense.formatAsCurrency())

                CardDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LendBorrowItem("Owe to you", summary.totalLent.formatAsCurrency())
                    LendBorrowItem("You owe", summary.totalBorrowed.formatAsCurrency(), alignEnd = true)
                }
            }

            SummaryCard(modifier = Modifier.weight(1f)) {
                CardSectionLabel("Net worth")
                Spacer(Modifier.height(8.dp))
                CompactRow("Net worth", summary.netWorth.formatAsCurrency(), FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                CompactRow("Assets", summary.totalAssets.formatAsCurrency())
                Spacer(Modifier.height(4.dp))
                CompactRow("Liabilities", summary.totalLiabilities.formatAsCurrency())

                CardDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Health",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$healthScore%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (healthScore > 70) MaterialTheme.colorScheme.tertiary
                        else if (healthScore > 40) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { healthScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (healthScore > 70) MaterialTheme.colorScheme.tertiary
                    else if (healthScore > 40) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = filter.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─── Header sub-components ────────────────────────────────────────────────────

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(6.dp), content = content)
    }
}

@Composable
private fun CardSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
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
private fun CompactRow(
    label: String,
    value: String,
    fontWeight: FontWeight = FontWeight.Medium,

) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
        )
    }
}

@Composable
private fun RowScope.LendBorrowItem(label: String, value: String, alignEnd: Boolean = false) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DistributionSection(
    distribution: Map<String, Double>,
    allCategories: List<CategoryEntity>,
    modifier: Modifier = Modifier
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

        Text(
            text = "Spending Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .clip(RoundedCornerShape(8.dp))
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
                                shape = RoundedCornerShape(8.dp)
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
            shape = MaterialTheme.shapes.extraLarge,
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