package com.example.expncetracker.exptkr.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_RECENT_TRANSACTIONS = 15

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
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is DashboardUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
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
    // FIX: group once outside LazyColumn so keys are stable
    val groupedRecent = remember(recent) {
        recent.take(MAX_RECENT_TRANSACTIONS)
            .groupBy { tx ->
                tx.timestamp.format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
                )
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // FIX: removed verticalArrangement spacedBy — it conflicts with per-item
        // padding and produces doubled gaps. Use explicit Spacer items instead.
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        item(key = "header") {
            ModernDashboardHeader(
                summary = summary,
                recurringCount = recurring.size,
                currentFilter = currentFilter,
                onFilterChange = onFilterChange
            )
        }

        item(key = "distribution") {
            Spacer(Modifier.height(8.dp))
            DistributionSection(
                distribution = summary.categoryDistribution,
                allCategories = categories,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (recurring.isNotEmpty()) {
            item(key = "upcoming_header") {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "Upcoming payments",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(recurring, key = { "rec_${it.id}" }) { tx ->
                val categoryEntity = categories.find { it.name == tx.categoryName }
                TransactionListItem(
                    transaction = tx.copy(timestamp = tx.nextDueDate ?: tx.timestamp),
                    categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                    categoryColor = categoryEntity?.let { Color(it.color) },
                    onClick = null
                )
            }
        }

        // FIX: guard chart rendering — only show section when data exists
        if (trends.isNotEmpty()) {
            item(key = "trend_chart") {
                Spacer(Modifier.height(8.dp))
                SpendingTrendSection(trends = trends)
            }
        }

        if (goals.isNotEmpty()) {
            item(key = "goals_header") {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "Savings goals",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "goals_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    goals.forEach { goal -> GoalProgressItem(goal = goal) }
                }
            }
        }

        item(key = "recent_header") {
            Spacer(Modifier.height(8.dp))
            SectionHeader(
                title = "Recent activity",
                actionLabel = "See all",
                onActionClick = onSeeAllClick,
                modifier = Modifier.padding(horizontal = 16.dp)
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
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add first transaction")
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // FIX: use stable string keys — "date_<value>" prevents collision
            // with other item keys in this same LazyColumn
            groupedRecent.forEach { (date, transactions) ->
                item(key = "date_$date") {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp
                        )
                    )
                }
                items(transactions, key = { "tx_${it.id}" }) { transaction ->
                    val categoryEntity = categories.find { it.name == transaction.categoryName }
                    TransactionListItem(
                        transaction = transaction,
                        categoryIcon = categoryEntity?.let { getIconByName(it.iconName) },
                        categoryColor = categoryEntity?.let { Color(it.color) },
                        onClick = null
                    )
                }
            }
        }
    }
}

// ─── SpendingTrendSection ──────────────────────────────────────────────────────

@Composable
private fun SpendingTrendSection(trends: List<SpendingTrend>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trends) {
        modelProducer.runTransaction {
            lineSeries { series(trends.map { it.amount.toFloat() }) }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Spending trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            CartesianChartHost(
                chart = rememberCartesianChart(rememberLineCartesianLayer()),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

// ─── GoalProgressItem ─────────────────────────────────────────────────────────

@Composable
fun GoalProgressItem(goal: GoalEntity) {
    val progress = if (goal.targetAmount > 0)
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    else 0f

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
            // FIX: Canvas sized to match Box exactly so arc is never clipped;
            // stroke radius = (size/2 - strokeWidth/2) keeps it inside bounds
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    val inset = 4.dp.toPx() / 2f
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    // track
                    drawArc(
                        color = Color(goal.color).copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke
                    )
                    // fill
                    drawArc(
                        color = Color(goal.color),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke
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
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${goal.currentAmount.formatAsCurrency()} / ${goal.targetAmount.formatAsCurrency()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            goal.deadline?.let { deadlineMillis ->
                val deadlineDate = Instant.ofEpochMilli(deadlineMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                Text(
                    text = "Due ${deadlineDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── ModernDashboardHeader ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDashboardHeader(
    summary: FinancialSummary,
    recurringCount: Int,
    currentFilter: DateFilter,
    onFilterChange: (DateFilter) -> Unit
) {
    val now = LocalTime.now()
    val greeting = when (now.hour) {
        in 0..11 -> "Good morning"
        in 12..15 -> "Good afternoon"
        in 16..20 -> "Good evening"
        else -> "Good night"
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthYearFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.getDefault())
    }
    val healthScore = remember(summary) {
        if (summary.totalAssets > 0)
            ((summary.netWorth / summary.totalAssets) * 100).toInt().coerceIn(0, 100)
        else 0
    }

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

    Column(modifier = Modifier.padding(12.dp)) {

        // Row 1: greeting (left) + payment badge + filter (right)
        // FIX: removed duplicate month text from here — it already appears in Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recurringCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Payment due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                OutlinedIconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.size(32.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Row 2: month navigation (single source of month display)
        val isNextMonthInFuture = remember(yearMonth) {
            yearMonth.plusMonths(1).isAfter(YearMonth.now())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    yearMonth = yearMonth.minusMonths(1)
                    onFilterChange(DateFilter.MONTH)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = yearMonth.format(monthYearFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = {
                    yearMonth = yearMonth.plusMonths(1)
                    onFilterChange(DateFilter.MONTH)
                },
                modifier = Modifier.size(28.dp),
                enabled = !isNextMonthInFuture
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (isNextMonthInFuture)
                        MaterialTheme.colorScheme.outlineVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Row 3: summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(modifier = Modifier.weight(1f)) {
                CardSectionLabel("Cash flow")
                Spacer(Modifier.height(6.dp))
                CompactRow("Balance", summary.balance.formatAsCurrency())
                Spacer(Modifier.height(4.dp))
                CompactRow("Income", summary.totalIncome.formatAsCurrency(),
                    MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(4.dp))
                CompactRow("Expense", summary.totalExpense.formatAsCurrency(),
                    MaterialTheme.colorScheme.error)
                CardDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LendBorrowItem("Owe to you", summary.totalLent.formatAsCurrency())
                    LendBorrowItem("You owe", summary.totalBorrowed.formatAsCurrency(),
                        alignEnd = true)
                }
            }

            SummaryCard(modifier = Modifier.weight(1f)) {
                CardSectionLabel("Net worth")
                Spacer(Modifier.height(6.dp))
                CompactRow("Net worth", summary.netWorth.formatAsCurrency())
                Spacer(Modifier.height(4.dp))
                CompactRow("Assets", summary.totalAssets.formatAsCurrency(),
                    MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(4.dp))
                CompactRow("Liabilities", summary.totalLiabilities.formatAsCurrency(),
                    MaterialTheme.colorScheme.error)
                CardDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Health score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$healthScore%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { healthScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

// ─── Header sub-components ────────────────────────────────────────────────────

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp), content = content)
    }
}

@Composable
private fun CardSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.06.sp
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun CompactRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun LendBorrowItem(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─── DistributionSection ──────────────────────────────────────────────────────

data class DistributionItem(
    val name: String,
    val amount: Double,
    val category: Category,
    val color: Color
)

@Composable
fun DistributionSection(
    distribution: Map<String, Double>,
    allCategories: List<CategoryEntity>,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.isDark
    val distributionData = remember(distribution, allCategories, isDarkTheme) {
        allCategories.mapNotNull { entity ->
            val amount = distribution[entity.name] ?: return@mapNotNull null
            if (amount <= 0.0) return@mapNotNull null
            val categoryEnum = Category.entries.find { it.displayName == entity.name }
                ?: Category.OTHERS
            DistributionItem(entity.name, amount, categoryEnum,
                getCategoryColor(categoryEnum, isDarkTheme))
        }.sortedByDescending { it.amount }
    }

    if (distributionData.isEmpty()) return

    val maxVal = remember(distributionData) {
        distributionData.maxOf { it.amount }.coerceAtLeast(1.0)
    }

    Column(modifier = modifier) {
        Text(
            text = "Spending distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))

        // FIX: replaced the fragile rotated-label-inside-bar approach with a
        // clean bar + label-below layout. No more graphicsLayer rotation hacks,
        // no invisible text, no clipping issues.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            distributionData.forEach { item ->
                val fraction = (item.amount / maxVal).toFloat().coerceIn(0.02f, 1f)
                val barMaxHeight = 140.dp

                Column(
                    modifier = Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // amount label above bar
                    Text(
                        text = if (item.amount >= 1000)
                            String.format(Locale.US, "%.1fK", item.amount / 1000)
                        else
                            item.amount.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))

                    // bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barMaxHeight * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(item.color)
                    )

                    Spacer(Modifier.height(6.dp))

                    // category name below bar — truncated, readable, no rotation needed
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// ─── TimeFilterRow ────────────────────────────────────────────────────────────

@Composable
fun TimeFilterRow(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Select period",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // FIX: replaced weight(1f) FilterChips in a plain Row with a properly
        // bounded segmented layout. Each chip gets equal width via weight inside
        // a Row that is itself fillMaxWidth — this is what weight actually needs.
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        onClick = { onFilterSelected(filter) }
                    ) {
                        Text(
                            text = filter.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── getCategoryColor (unchanged) ─────────────────────────────────────────────

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

// ─── DashboardInfoItem (kept for any legacy call sites) ───────────────────────

@Composable
fun DashboardInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}