package com.example.expncetracker.exptkr.ui.analytics

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.ui.dashboard.DistributionSection
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// ─── Trend window for chart period selector ───────────────────────────────────

private enum class TrendWindow(val label: String, val days: Int) {
    W1("1W", 7),
    M1("1M", 30),
    M3("3M", 90),
    Y1("1Y", 365)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val summary by viewModel.summary.collectAsState()
    val previousSummary by viewModel.previousSummary.collectAsState()  // NEW: prior period for comparison
    val trends by viewModel.trends.collectAsState()
    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val isDark = MaterialTheme.isDark
    val locale = LocalConfiguration.current.locales[0]

    val pullRefreshState = rememberPullToRefreshState()
    val lineModelProducer = remember { CartesianChartModelProducer() }
    val columnModelProducer = remember { CartesianChartModelProducer() }

    // NEW: independent trend window for the line chart period selector
    var trendWindow by remember { mutableStateOf(TrendWindow.M1) }

    // NEW: category drill-down sheet state
    var drillDownCategory by remember { mutableStateOf<String?>(null) }
    val drillDownTransactions by remember(drillDownCategory, viewModel) {
        derivedStateOf {
            if (drillDownCategory == null) emptyList()
            else viewModel.getTransactionsForCategory(drillDownCategory!!)
        }
    }

    val today = LocalDate.now()
    var currentPeriodStart by remember {
        mutableStateOf(
            today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
        )
    }
    val weekEnd = currentPeriodStart.plusDays(6)
    val isViewingCurrentPeriod = remember(currentPeriodStart, selectedFilter) {
        when (selectedFilter) {
            DateFilter.DAY -> currentPeriodStart == today
            DateFilter.WEEK -> currentPeriodStart == today.with(
                WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L
            )
            DateFilter.WEEK_RANGE -> false
            DateFilter.MONTH -> currentPeriodStart.year == today.year &&
                    currentPeriodStart.monthValue == today.monthValue
            DateFilter.YEAR -> currentPeriodStart.year == today.year
        }
    }

    val axisLabel = rememberTextComponent(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val axisGuideline = rememberLineComponent(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
    )

    val lineChart = rememberCartesianChart(
        rememberLineCartesianLayer(),
        startAxis = rememberStartAxis(label = axisLabel, tick = null, guideline = axisGuideline),
        bottomAxis = rememberBottomAxis(
            label = axisLabel,
            tick = null,
            guideline = null,
            valueFormatter = { value, _, _ -> trends.getOrNull(value.toInt())?.label ?: "" }
        )
    )

    val columnChart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(
                    color = MaterialTheme.colorScheme.primary,
                    thickness = 10.dp,
                    shape = Shape.rounded(50)
                )
            )
        ),
        startAxis = rememberStartAxis(label = axisLabel, tick = null, guideline = axisGuideline),
        bottomAxis = rememberBottomAxis(
            label = axisLabel,
            valueFormatter = { value, _, _ ->
                // NEW: correct axis labels per filter
                when (selectedFilter) {
                    DateFilter.WEEK -> {
                        val day = currentPeriodStart.plusDays(value.toLong())
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    }
                    DateFilter.WEEK_RANGE -> {
                        val day = currentPeriodStart.plusDays(value.toLong())
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    }
                    DateFilter.MONTH -> "W${(value.toInt() + 1)}"
                    DateFilter.YEAR -> DateTimeFormatter.ofPattern("MMM").format(
                        java.time.Month.of((value.toInt() + 1).coerceIn(1, 12))
                    )
                    else -> value.toInt().toString()
                }
            }
        )
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(selectedFilter) {
        if (isInitialLoad) { isInitialLoad = false; return@LaunchedEffect }
        currentPeriodStart = when (selectedFilter) {
            DateFilter.DAY -> today
            DateFilter.WEEK -> today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
            DateFilter.WEEK_RANGE -> currentPeriodStart
            DateFilter.MONTH -> today.withDayOfMonth(1)
            DateFilter.YEAR -> LocalDate.of(today.year, 1, 1)
        }
    }

    LaunchedEffect(currentPeriodStart, selectedFilter) {
        if (selectedFilter == DateFilter.WEEK) {
            viewModel.setWeekRange(currentPeriodStart, weekEnd)
        }
        // NEW: also load previous period for comparison
        viewModel.loadPreviousPeriod(currentPeriodStart, selectedFilter)
    }

    LaunchedEffect(trends, dailyTotals, selectedFilter) {
        if (trends.isNotEmpty()) {
            lineModelProducer.runTransaction {
                lineSeries { series(trends.map { it.amount.toFloat() }) }
            }
        }
        if (dailyTotals.isNotEmpty()) {
            // NEW: correct grouping per filter type
                        val groupedData: List<Float> = when (selectedFilter) {
                            DateFilter.DAY, DateFilter.WEEK, DateFilter.WEEK_RANGE ->
                                dailyTotals.entries.sortedBy { it.key }.map { it.value.toFloat() }

                            DateFilter.MONTH -> {
                    // Group by week-of-month (4 buckets)
                    val weeks = (0..4).map { week ->
                        dailyTotals.entries
                            .filter { (d, _) -> (d.dayOfMonth - 1) / 7 == week }
                            .sumOf { it.value }
                            .toFloat()
                    }.dropLastWhile { it == 0f }
                    weeks
                }

                DateFilter.YEAR -> {
                    // Group by month (12 buckets)
                    (1..12).map { month ->
                        dailyTotals.entries
                            .filter { (d, _) -> d.monthValue == month }
                            .sumOf { it.value }
                            .toFloat()
                    }
                }
            }
            if (groupedData.isNotEmpty()) {
                columnModelProducer.runTransaction {
                    columnSeries { series(groupedData) }
                }
            }
        }
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }

    if (showFilterSheet) {
        // Obsolete sheet removed
    }

    // NEW: Category drill-down sheet
    if (drillDownCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { drillDownCategory = null },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        drillDownCategory ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    val catAmount = summary?.categoryDistribution?.get(drillDownCategory) ?: 0.0
                    Text(
                        catAmount.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                if (drillDownTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    drillDownTransactions.forEach { tx ->
                        val catEntity = allCategories.find { it.name == tx.categoryName }
                        TransactionListItem(
                            transaction = tx,
                            categoryIcon = catEntity?.let { getIconByName(it.iconName) },
                            categoryColor = catEntity?.let { Color(it.color) },
                            onClick = null
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── 1. Summary header card ─────────────────────────────────────
            item(key = "summary_header") {
                AnalyticsCard {
                    // Unified Filter and Navigation Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Day
                        FilterItemSimple(
                            "Day",
                            selectedFilter == DateFilter.DAY,
                            onClick = { viewModel.setFilter(DateFilter.DAY) },
                            modifier = Modifier.weight(0.7f)
                        )

                        // 2. Week
                        FilterItemSimple(
                            "Week",
                            selectedFilter == DateFilter.WEEK,
                            onClick = { viewModel.setFilter(DateFilter.WEEK) },
                            modifier = Modifier.weight(0.7f)
                        )

                        // 3. Date Navigation (In place of Week Range)
                        Row(
                            modifier = Modifier.weight(2.5f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    currentPeriodStart = when (selectedFilter) {
                                        DateFilter.DAY -> currentPeriodStart.minusDays(1)
                                        DateFilter.WEEK -> currentPeriodStart.minusWeeks(1)
                                        DateFilter.WEEK_RANGE -> currentPeriodStart.minusWeeks(1)
                                        DateFilter.MONTH -> currentPeriodStart.minusMonths(1).withDayOfMonth(1)
                                        DateFilter.YEAR -> LocalDate.of(currentPeriodStart.year - 1, 1, 1)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            val dateText = when (selectedFilter) {
                                DateFilter.DAY -> currentPeriodStart.format(dayFormatter)
                                DateFilter.WEEK -> "${currentPeriodStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
                                DateFilter.WEEK_RANGE -> "${currentPeriodStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
                                DateFilter.MONTH -> currentPeriodStart.format(monthFormatter)
                                DateFilter.YEAR -> currentPeriodStart.year.toString()
                            }

                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (selectedFilter == DateFilter.WEEK_RANGE) showRangePicker = true
                                        else showDatePicker = true
                                    }
                            )

                            val isNextDisabled = when (selectedFilter) {
                                DateFilter.DAY -> !currentPeriodStart.isBefore(today)
                                DateFilter.WEEK -> !currentPeriodStart.plusWeeks(1).isBefore(
                                    today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L).plusDays(1)
                                )
                                DateFilter.WEEK_RANGE -> false
                                DateFilter.MONTH -> currentPeriodStart.year == today.year &&
                                        currentPeriodStart.monthValue >= today.monthValue
                                DateFilter.YEAR -> currentPeriodStart.year >= today.year
                            }

                            IconButton(
                                onClick = {
                                    currentPeriodStart = when (selectedFilter) {
                                        DateFilter.DAY -> currentPeriodStart.plusDays(1)
                                        DateFilter.WEEK -> currentPeriodStart.plusWeeks(1)
                                        DateFilter.WEEK_RANGE -> currentPeriodStart.plusWeeks(1)
                                        DateFilter.MONTH -> currentPeriodStart.plusMonths(1).withDayOfMonth(1)
                                        DateFilter.YEAR -> LocalDate.of(currentPeriodStart.year + 1, 1, 1)
                                    }
                                },
                                enabled = !isNextDisabled,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null,
                                    tint = if (isNextDisabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 4. Month
                        FilterItemSimple(
                            "Month",
                            selectedFilter == DateFilter.MONTH,
                            onClick = { viewModel.setFilter(DateFilter.MONTH) },
                            modifier = Modifier.weight(0.8f)
                        )

                        // 5. Year
                        FilterItemSimple(
                            "Year",
                            selectedFilter == DateFilter.YEAR,
                            onClick = { viewModel.setFilter(DateFilter.YEAR) },
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Jump to today (Small text button below row if navigated away)
                    AnimatedVisibility(visible = !isViewingCurrentPeriod && selectedFilter != DateFilter.WEEK_RANGE) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(
                                onClick = {
                                    currentPeriodStart = when (selectedFilter) {
                                        DateFilter.DAY -> today
                                        DateFilter.WEEK -> today.with(
                                            WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L
                                        )
                                        DateFilter.MONTH -> today.withDayOfMonth(1)
                                        DateFilter.YEAR -> LocalDate.of(today.year, 1, 1)
                                        else -> today
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Today, null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Today", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    // NEW: three stat columns with period-over-period delta
                    val expense = summary?.totalExpense ?: 0.0
                    val income = summary?.totalIncome ?: 0.0
                    val balance = summary?.balance ?: 0.0
                    val prevExpense = previousSummary?.totalExpense ?: 0.0
                    val prevIncome = previousSummary?.totalIncome ?: 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        SummaryStatCell(
                            label = "Expense",
                            value = expense.formatAsCurrency(),
                            valueColor = MaterialTheme.colorScheme.error,
                            previousValue = prevExpense,
                            currentValue = expense,
                            higherIsBetter = false,
                            modifier = Modifier.weight(1f)
                        )
                        VerticalStatDivider()
                        SummaryStatCell(
                            label = "Income",
                            value = income.formatAsCurrency(),
                            valueColor = MaterialTheme.colorScheme.tertiary,
                            previousValue = prevIncome,
                            currentValue = income,
                            higherIsBetter = true,
                            modifier = Modifier.weight(1f)
                        )
                        VerticalStatDivider()
                        SummaryStatCell(
                            label = "Balance",
                            value = balance.formatAsCurrency(),
                            valueColor = if (balance >= 0) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // NEW: budget progress bar (only shown when budget is set)
                    summary?.budget?.let { budget ->
                        if (budget > 0) {
                            val fraction = (expense / budget).toFloat().coerceIn(0f, 1f)
                            val overBudget = expense > budget
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Budget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${expense.formatAsCurrency()} / ${budget.formatAsCurrency()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overBudget) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (overBudget) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            if (overBudget) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Over budget by ${(expense - budget).formatAsCurrency()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Weekly day grid ─────────────────────────────────────────
            if (selectedFilter == DateFilter.WEEK) {
                item(key = "weekly_grid") {
                    AnalyticsCard {
                        Text(
                            "Daily breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(12.dp))
                        val days = (0..6).map { currentPeriodStart.plusDays(it.toLong()) }
                        val maxAmount = dailyTotals.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            days.forEach { day ->
                                val amount = dailyTotals[day] ?: 0.0
                                val fraction = (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                                val isToday = day == today

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                    ) {
                                        Text(
                                            text = day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    // Proportional mini bar
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (fraction > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(fraction)
                                                    .align(Alignment.BottomCenter)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        if (isToday) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.error
                                                    )
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (amount > 0)
                                            amount.formatAsCurrency().replace("₹", "")
                                        else "—",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (amount > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Spending trend with period selector ─────────────────────
            item(key = "trend_chart") {
                AnalyticsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Spending trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        // NEW: window selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TrendWindow.entries.forEach { window ->
                                val isSelected = trendWindow == window
                                Surface(
                                    onClick = {
                                        trendWindow = window
                                        viewModel.loadTrends(window.days)
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    border = if (isSelected) null
                                    else BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = window.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (trends.isNotEmpty()) {
                        // NEW: quick total above chart
                        val trendTotal = trends.sumOf { it.amount }
                        Text(
                            text = trendTotal.formatAsCurrency(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        CartesianChartHost(
                            chart = lineChart,
                            modelProducer = lineModelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    } else {
                        ChartEmptyState("No trend data for this period")
                    }
                }
            }

            // ── 4. Category distribution with drill-down ───────────────────
            val dist = summary?.categoryDistribution
            if (!dist.isNullOrEmpty()) {
                item(key = "distribution") {
                    AnalyticsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Spending distribution",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Tap to drill down",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        // Pass drill-down callback into DistributionSection
                        DistributionSection(
                            distribution = dist,
                            allCategories = allCategories,
                            modifier = Modifier.fillMaxWidth(),
                            onCategoryClick = { categoryName ->
                                drillDownCategory = categoryName
                            }
                        )

                        // NEW: top spender insight callout
                        val topCategory = dist.maxByOrNull { it.value }
                        val totalSpend = dist.values.sum()
                        topCategory?.let { (name, amount) ->
                            if (totalSpend > 0) {
                                val pct = ((amount / totalSpend) * 100).toInt()
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "$name is your top spend at $pct% of total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. Performance bar chart ───────────────────────────────────
            item(key = "performance_chart") {
                AnalyticsCard {
                    val perfLabel = when (selectedFilter) {
                        DateFilter.WEEK -> "Daily spending"
                        DateFilter.MONTH -> "Weekly spending"
                        DateFilter.YEAR -> "Monthly spending"
                        else -> "Performance"
                    }
                    Text(
                        perfLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    if (dailyTotals.isNotEmpty()) {
                        CartesianChartHost(
                            chart = columnChart,
                            modelProducer = columnModelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    } else {
                        ChartEmptyState("No data for this period")
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.setCustomRange(start, end)
                    }
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
                title = { Text("Select Week Range", modifier = Modifier.padding(16.dp)) },
                headline = { 
                    Text(
                        text = "Custom Range", 
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                showModeToggle = false
            )
        }
    }

    // ── Date picker ────────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentPeriodStart
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        currentPeriodStart = when (selectedFilter) {
                            DateFilter.DAY -> selected
                            DateFilter.WEEK -> selected.with(
                                WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L
                            )
                            DateFilter.WEEK_RANGE -> selected
                            DateFilter.MONTH -> selected.withDayOfMonth(1)
                            DateFilter.YEAR -> LocalDate.of(selected.year, 1, 1)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                        title = {
                            Text(
                                text = when (selectedFilter) {
                                    DateFilter.DAY -> "Select date"
                                    DateFilter.WEEK -> "Select week"
                                    DateFilter.WEEK_RANGE -> "Select range"
                                    DateFilter.MONTH -> "Select month"
                                    DateFilter.YEAR -> "Select year"
                                },
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                            )
                        }
            )
        }
    }
}

@Composable
private fun FilterItemSimple(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─── AnalyticsCard ────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ─── SummaryStatCell with period delta ────────────────────────────────────────

@Composable
private fun SummaryStatCell(
    label: String,
    value: String,
    valueColor: Color,
    previousValue: Double? = null,
    currentValue: Double? = null,
    higherIsBetter: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(180))) },
            label = "StatValue"
        ) { v ->
            Text(
                text = v,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = if (value.length > 10) 12.sp else 15.sp
            )
        }

        // NEW: period comparison delta
        if (previousValue != null && currentValue != null && previousValue > 0) {
            val delta = ((currentValue - previousValue) / previousValue) * 100
            val isPositive = delta > 0
            val isGood = if (higherIsBetter) isPositive else !isPositive
            val deltaColor = if (isGood) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.error
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        delta > 1.0 -> Icons.AutoMirrored.Filled.TrendingUp
                        delta < -1.0 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = deltaColor
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.1f", delta)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = deltaColor
                )
            }
        }
    }
}

// ─── VerticalStatDivider ──────────────────────────────────────────────────────

@Composable
private fun VerticalStatDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ─── ChartEmptyState ──────────────────────────────────────────────────────────

@Composable
private fun ChartEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Default.BarChart,
                null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}