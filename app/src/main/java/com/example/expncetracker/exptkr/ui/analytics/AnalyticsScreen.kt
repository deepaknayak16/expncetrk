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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.expncetracker.exptkr.ui.components.DistributionSection
import com.example.expncetracker.exptkr.ui.components.SpendingTrendSection
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as TimeTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

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
    val allTransactions by viewModel.currentTransactions.collectAsState()
    val drillDownTransactions by remember(drillDownCategory, allTransactions) {
        derivedStateOf {
            if (drillDownCategory == null) emptyList()
            else allTransactions.filter { it.categoryName == drillDownCategory }
        }
    }

    val lineScrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    val columnScrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)

    // Ensure charts stay at the end when data updates
    LaunchedEffect(trends) {
        if (trends.isNotEmpty()) {
            // Give a small delay for the chart to process the model update
            kotlinx.coroutines.delay(100)
            lineScrollState.scroll(Scroll.Absolute.End)
        }
    }

    LaunchedEffect(dailyTotals, selectedFilter) {
        if (dailyTotals.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            columnScrollState.scroll(Scroll.Absolute.End)
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

    val axisLabel = rememberTextComponent(style = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant))
    val axisGuideline = rememberLineComponent(
        fill = Fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    )

    val lineChart = key(trends) {
        rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    listOf(
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary))),
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFFF44336)))),
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFF4CAF50)))),
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFFFF9800)))),
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFF9C27B0)))),
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                tick = null,
                guideline = axisGuideline
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                tick = null,
                guideline = null,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                    spacing = { 1 },
                    addExtremeLabelPadding = true
                ),
                valueFormatter = { _, value, _ -> 
                    val label = trends["Total"]?.getOrNull(value.toInt())?.label 
                    if (label.isNullOrBlank()) "\u2022" else label
                }
            ),
            getXStep = { _, _, _ -> if (trendWindow == TrendWindow.W1) 1.0 else 1.0 }
        )
    }

    val columnChart = key(selectedFilter, currentPeriodStart) {
        rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(MaterialTheme.colorScheme.primary),
                        thickness = 10.dp,
                        shape = RoundedCornerShape(percent = 50)
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                label = axisLabel,
                tick = null,
                guideline = axisGuideline
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                valueFormatter = { _, value, _ ->
                    // NEW: correct axis labels per filter
                    val label = when (selectedFilter) {
                        DateFilter.WEEK -> {
                            val day = currentPeriodStart.plusDays(value.toLong())
                            day.dayOfWeek.getDisplayName(TimeTextStyle.SHORT, locale)
                        }
                        DateFilter.WEEK_RANGE -> {
                            val day = currentPeriodStart.plusDays(value.toLong())
                            day.dayOfWeek.getDisplayName(TimeTextStyle.SHORT, locale)
                        }
                        DateFilter.MONTH -> "W${(value.toInt() + 1)}"
                        DateFilter.YEAR -> {
                            val month = (value.toInt() + 1).coerceIn(1, 12)
                            DateTimeFormatter.ofPattern("MMM").format(java.time.Month.of(month))
                        }
                        else -> value.toInt().toString()
                    }
                    if (label.isNullOrBlank()) "\u2022" else label
                }
            ),
            getXStep = { _, _, _ -> 1.0 }
        )
    }

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
        // Synchronize the ViewModel's range with the UI's selected period
        val end = when (selectedFilter) {
            DateFilter.DAY -> currentPeriodStart
            DateFilter.WEEK -> currentPeriodStart.plusDays(6)
            DateFilter.WEEK_RANGE -> currentPeriodStart.plusDays(6)
            DateFilter.MONTH -> currentPeriodStart.withDayOfMonth(currentPeriodStart.lengthOfMonth())
            DateFilter.YEAR -> currentPeriodStart.withDayOfYear(currentPeriodStart.lengthOfYear())
        }
        
        // Use a more specific method or handle filter update carefully to avoid race
        viewModel.setWeekRange(currentPeriodStart, end)
        
        // Ensure filter is also synced if changed locally
        if (viewModel.selectedFilter.value != selectedFilter) {
            // Note: setFilter might also update range, which could cause double jump.
            // Better to let ViewModel handle the source of truth for filter.
        }

        // Load previous period for comparison
        viewModel.loadPreviousPeriod(currentPeriodStart, selectedFilter)
    }

    LaunchedEffect(trends, dailyTotals, selectedFilter) {
        if (trends.isNotEmpty()) {
            lineModelProducer.runTransaction {
                lineSeries { 
                    trends.values.forEach { series(it.map { p -> p.amount.toFloat() }) }
                }
            }
        }
        
        // NEW: robust grouping logic for Month/Year data that prevents hole skipping
        val groupedData: List<Float> = when (selectedFilter) {
            DateFilter.DAY -> {
                listOf(dailyTotals[currentPeriodStart]?.toFloat() ?: 0f)
            }
            DateFilter.WEEK, DateFilter.WEEK_RANGE -> {
                (0..6).map { i ->
                    val date = currentPeriodStart.plusDays(i.toLong())
                    dailyTotals[date]?.toFloat() ?: 0f
                }
            }

            DateFilter.MONTH -> {
                // Always create 5 buckets (weeks) to keep chart stable
                val startOfMonth = currentPeriodStart.withDayOfMonth(1)
                (0..4).map { week ->
                    val weekStart = startOfMonth.plusDays(week * 7L)
                    val weekEnd = weekStart.plusDays(6)
                    dailyTotals.filter { (d, _) -> !d.isBefore(weekStart) && !d.isAfter(weekEnd) }
                        .values.sumOf { it }.toFloat()
                }
            }

            DateFilter.YEAR -> {
                // Always create 12 buckets (months) to keep chart stable
                (1..12).map { month ->
                    dailyTotals.filter { (d, _) -> d.monthValue == month && d.year == currentPeriodStart.year }
                        .values.sumOf { it }.toFloat()
                }
            }
        }

        // Always update the producer, even if data is empty, to clear the previous view
        columnModelProducer.runTransaction {
            columnSeries { 
                if (groupedData.isNotEmpty()) series(groupedData) 
                else series(List(if (selectedFilter == DateFilter.YEAR) 12 else 7) { 0f })
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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
                    val catAmount = summary?.categoryDistribution?.get(drillDownCategory)?.toDouble() ?: 0.0
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

                    // ─────────────────────────────
                    // FILTER + NAV ROW (COMPACT)
                    // ─────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FilterItemSimple(
                            "Daily",
                            selectedFilter == DateFilter.DAY,
                            { viewModel.setFilter(DateFilter.DAY) },
                            Modifier.weight(0.7f)
                        )

                        FilterItemSimple(
                            "Week",
                            selectedFilter == DateFilter.WEEK,
                            { viewModel.setFilter(DateFilter.WEEK) },
                            Modifier.weight(0.7f)
                        )

                        FilterItemSimple(
                            "Month",
                            selectedFilter == DateFilter.MONTH,
                            { viewModel.setFilter(DateFilter.MONTH) },
                            Modifier.weight(0.7f)
                        )

                        FilterItemSimple(
                            "Year",
                            selectedFilter == DateFilter.YEAR,
                            { viewModel.setFilter(DateFilter.YEAR) },
                            Modifier.weight(0.7f)
                        )

                        Spacer(Modifier.width(4.dp))

                        // ───────── DATE NAV ─────────
                        val dateText = when (selectedFilter) {
                            DateFilter.DAY -> currentPeriodStart.format(dayFormatter)
                            DateFilter.WEEK -> "${currentPeriodStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
                            DateFilter.WEEK_RANGE -> "${currentPeriodStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
                            DateFilter.MONTH -> currentPeriodStart.format(monthFormatter)
                            DateFilter.YEAR -> currentPeriodStart.year.toString()
                        }

                        val isNextDisabled = when (selectedFilter) {
                            DateFilter.DAY -> !currentPeriodStart.isBefore(today)
                            DateFilter.WEEK -> !currentPeriodStart.plusWeeks(1).isBefore(
                                today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L).plusDays(1)
                            )
                            DateFilter.WEEK_RANGE -> false
                            DateFilter.MONTH ->
                                currentPeriodStart.year == today.year &&
                                        currentPeriodStart.monthValue >= today.monthValue
                            DateFilter.YEAR ->
                                currentPeriodStart.year >= today.year
                        }

                        IconButton(
                            onClick = {
                                currentPeriodStart = when (selectedFilter) {
                                    DateFilter.DAY -> currentPeriodStart.minusDays(1)
                                    DateFilter.WEEK -> currentPeriodStart.minusWeeks(1)
                                    DateFilter.WEEK_RANGE -> currentPeriodStart.minusWeeks(1)
                                    DateFilter.MONTH -> currentPeriodStart.minusMonths(1).withDayOfMonth(1)
                                    DateFilter.YEAR -> currentPeriodStart.minusYears(1)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                null,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    if (selectedFilter == DateFilter.WEEK_RANGE)
                                        showRangePicker = true
                                    else
                                        showDatePicker = true
                                }
                        )

                        IconButton(
                            onClick = {
                                currentPeriodStart = when (selectedFilter) {
                                    DateFilter.DAY -> currentPeriodStart.plusDays(1)
                                    DateFilter.WEEK -> currentPeriodStart.plusWeeks(1)
                                    DateFilter.WEEK_RANGE -> currentPeriodStart.plusWeeks(1)
                                    DateFilter.MONTH -> currentPeriodStart.plusMonths(1).withDayOfMonth(1)
                                    DateFilter.YEAR -> currentPeriodStart.plusYears(1)
                                }
                            },
                            enabled = !isNextDisabled,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = if (isNextDisabled)
                                    MaterialTheme.colorScheme.outlineVariant
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(6.dp))

                    // ─────────────────────────────
                    // SUMMARY
                    // ─────────────────────────────

                    val expense = summary?.totalExpense?.toDouble() ?: 0.0
                    val income = summary?.totalIncome?.toDouble() ?: 0.0
                    val balance = summary?.balance?.toDouble() ?: 0.0

                    val prevExpense = previousSummary?.totalExpense?.toDouble() ?: 0.0
                    val prevIncome = previousSummary?.totalIncome?.toDouble() ?: 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        SummaryStatCell(
                            label = "Expense",
                            style = MaterialTheme.typography.titleSmall,
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
                            style = MaterialTheme.typography.titleSmall,
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
                            style = MaterialTheme.typography.titleSmall,
                            value = balance.formatAsCurrency(),
                            valueColor =
                                if (balance >= 0)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // ─────────────────────────────
                    // BUDGET (COMPACTED SPACING ONLY)
                    // ─────────────────────────────
                    summary?.budget?.let { budgetBigDecimal ->
                        val budget = budgetBigDecimal.toDouble()
                        if (budget > 0.0) {

                            val fraction = (expense / budget).toFloat().coerceIn(0f, 1f)
                            val overBudget = expense > budget

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Budget", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${expense.formatAsCurrency()} / ${budget.formatAsCurrency()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overBudget)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(3.dp))

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (overBudget)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
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
                        val days = remember(currentPeriodStart) { 
                            (0..6).map { currentPeriodStart.plusDays(it.toLong()) } 
                        }
                        val maxAmount by remember(dailyTotals) {
                            derivedStateOf { dailyTotals.values.maxOrNull()?.toDouble()?.coerceAtLeast(1.0) ?: 1.0 }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            days.forEach { day ->
                                val amount = dailyTotals[day]?.toDouble() ?: 0.0
                                val fraction = (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                                val isToday = day == today

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = day.dayOfWeek.getDisplayName(TimeTextStyle.NARROW, locale),
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
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize() // NEW: Smooth out changes
                                                .fillMaxHeight(fraction.coerceIn(0.01f, 1f))
                                                .align(Alignment.BottomCenter)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (amount > 0) {
                                                        if (isToday) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.error
                                                    } else Color.Transparent
                                                )
                                        )
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
                key(trends) {
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
                            val trendTotal = trends["Total"]?.sumOf { it.amount }?.toDouble() ?: 0.0
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
                                scrollState = lineScrollState,
                                zoomState = rememberVicoZoomState(
                                    initialZoom = if (trendWindow == TrendWindow.W1) Zoom.Content 
                                                 else Zoom.x(7.0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                            
                            // NEW: simple legend
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                maxItemsInEachRow = 3
                            ) {
                                val colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFFF44336),
                                    Color(0xFF4CAF50),
                                    Color(0xFFFF9800),
                                    Color(0xFF9C27B0)
                                )
                                trends.keys.forEachIndexed { index, cat ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colors.getOrElse(index) { Color.Gray })
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            ChartEmptyState("No trend data for this period")
                        }
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
                            showTitle = false,
                            showCard = false,
                            onCategoryClick = { categoryName ->
                                drillDownCategory = categoryName
                            }
                        )

                        // NEW: top spender insight callout
                        val topCategory = dist.maxByOrNull { it.value }
                        val totalSpend = dist.values.sumOf { it }
                        topCategory?.let { (name, amount) ->
                            if (totalSpend > BigDecimal.ZERO) {
                                val pct = (amount.toDouble() / totalSpend.toDouble() * 100).toInt()
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
                            scrollState = columnScrollState,
                            zoomState = rememberVicoZoomState(
                                initialZoom = Zoom.Content
                            ),
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
        modifier = modifier.height(34.dp),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
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
    style: androidx.compose.ui.text.TextStyle? = null,
    modifier: Modifier = Modifier
) {

    val resolvedStyle = style ?: MaterialTheme.typography.titleSmall

    val locale = remember { java.util.Locale.getDefault() }

    val hasDelta = previousValue != null && currentValue != null && previousValue > 0

    val delta = if (hasDelta) {
        ((currentValue!! - previousValue!!) / previousValue) * 100
    } else 0.0

    val isPositive = delta > 0
    val isGood = if (higherIsBetter) isPositive else !isPositive

    val deltaColor = if (isGood)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val deltaText = remember(delta) {
        "${if (delta > 0) "+" else ""}${"%.1f".format(delta)}%"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = value,
            transitionSpec = {
                fadeIn(tween(150)).togetherWith(fadeOut(tween(150)))
            },
            label = "value_anim"
        ) { v ->

            Text(
                text = v,
                style = resolvedStyle,
                fontWeight = FontWeight.Bold,
                color = valueColor.copy(alpha = pulseAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasDelta) {

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
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = deltaColor
                )
            }

        } else {

            Spacer(Modifier.height(2.dp))

            Text(
                text = "—",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
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