package com.example.expncetracker.exptkr.ui.analytics

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.ui.dashboard.DistributionSection
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.ui.theme.*
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val summary by viewModel.summary.collectAsState()
    val trends by viewModel.trends.collectAsState()
    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val isDark = MaterialTheme.isDark
    
    val pullRefreshState = rememberPullToRefreshState()
    val lineModelProducer = remember { CartesianChartModelProducer() }
    val columnModelProducer = remember { CartesianChartModelProducer() }

    var currentWeekStart by remember {
        mutableStateOf(
            LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
        )
    }
    val weekEnd = currentWeekStart.plusDays(6)

    var showDatePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(selectedFilter) {
        if (isInitialLoad) {
            isInitialLoad = false
            return@LaunchedEffect
        }
        if (selectedFilter == DateFilter.DAY) {
            currentWeekStart = LocalDate.now()
        }
    }

    LaunchedEffect(currentWeekStart, selectedFilter) {
        if (selectedFilter == DateFilter.WEEK) {
            viewModel.setWeekRange(currentWeekStart, weekEnd)
        }
    }

    LaunchedEffect(trends) {
        if (trends.isNotEmpty()) {
            lineModelProducer.runTransaction { lineSeries { series(trends.map { it.amount.toFloat() }) } }
            columnModelProducer.runTransaction { columnSeries { series(trends.map { it.amount.toFloat() }) } }
        }
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.getDefault()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                TimeFilterRow(
                    currentFilter = selectedFilter,
                    onFilterSelected = { 
                        viewModel.setFilter(it)
                        showFilterSheet = false 
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Compact Summary Header (Redesigned like Dashboard)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Navigation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                currentWeekStart = when(selectedFilter) {
                                    DateFilter.DAY -> currentWeekStart.minusDays(1)
                                    DateFilter.WEEK -> currentWeekStart.minusWeeks(1)
                                    DateFilter.MONTH -> currentWeekStart.minusMonths(1).withDayOfMonth(1)
                                    DateFilter.YEAR -> currentWeekStart.minusYears(1).withDayOfYear(1)
                                }
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                val dateText = when(selectedFilter) {
                                    DateFilter.DAY -> currentWeekStart.format(dayFormatter)
                                    DateFilter.WEEK -> "${currentWeekStart.format(dateFormatter)} - ${weekEnd.format(dateFormatter)}"
                                    DateFilter.MONTH -> currentWeekStart.format(monthFormatter)
                                    DateFilter.YEAR -> currentWeekStart.year.toString()
                                }
                                AnimatedContent(targetState = dateText, label = "HeaderAnim") { text ->
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            Row {
                                val isNextDisabled = when(selectedFilter) {
                                    DateFilter.DAY -> currentWeekStart.isAfter(LocalDate.now().minusDays(1))
                                    DateFilter.WEEK -> currentWeekStart.plusDays(7).isAfter(LocalDate.now())
                                    DateFilter.MONTH -> currentWeekStart.plusMonths(1).isAfter(LocalDate.now().withDayOfMonth(1).minusDays(1))
                                    DateFilter.YEAR -> currentWeekStart.year >= LocalDate.now().year
                                }
                                IconButton(
                                    onClick = { 
                                        currentWeekStart = when(selectedFilter) {
                                            DateFilter.DAY -> currentWeekStart.plusDays(1)
                                            DateFilter.WEEK -> currentWeekStart.plusWeeks(1)
                                            DateFilter.MONTH -> currentWeekStart.plusMonths(1).withDayOfMonth(1)
                                            DateFilter.YEAR -> currentWeekStart.plusYears(1).withDayOfYear(1)
                                        }
                                    },
                                    enabled = !isNextDisabled
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight, 
                                        contentDescription = "Next", 
                                        tint = if (isNextDisabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AnalyticsSummaryColumn("EXPENSE", (summary?.totalExpense ?: 0.0).formatAsCurrency(), if (isDark) DarkExpense else LightExpense, Modifier.weight(1f))
                            AnalyticsSummaryColumn("INCOME", (summary?.totalIncome ?: 0.0).formatAsCurrency(), if (isDark) DarkIncome else LightIncome, Modifier.weight(1f))
                            AnalyticsSummaryColumn("TOTAL", (summary?.balance ?: 0.0).formatAsCurrency(), 
                                if ((summary?.balance ?: 0.0) >= 0) (if (isDark) DarkIncome else LightIncome) else (if (isDark) DarkExpense else LightExpense), 
                                Modifier.weight(1f))
                        }
                    }
                }
            }

            // 2. Weekly Activity Grid
            if (selectedFilter == DateFilter.WEEK) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Daily Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(16.dp))
                            val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                            val days = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                days.forEach { day ->
                                    val amount = dailyTotals[day] ?: 0.0
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (amount > 0) MaterialTheme.colorScheme.error else Color.Transparent)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (amount > 0) "-${amount.toInt()}" else "—",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (amount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Spending Trends
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Growth Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            if (trends.isNotEmpty()) {
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        rememberLineCartesianLayer(),
                                        startAxis = rememberStartAxis(
                                            label = rememberTextComponent(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            tick = null,
                                            guideline = rememberLineComponent(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        ),
                                        bottomAxis = rememberBottomAxis(
                                            label = rememberTextComponent(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            tick = null,
                                            guideline = null,
                                            valueFormatter = { value, _, _ -> trends.getOrNull(value.toInt())?.label ?: "" }
                                        )
                                    ),
                                    modelProducer = lineModelProducer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No trend data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Category Distribution
            item {
                summary?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (it.categoryDistribution.isNotEmpty()) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                DistributionSection(distribution = it.categoryDistribution, allCategories = allCategories)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No spending data to display", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 5. Monthly Comparison
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Performance View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(16.dp))
                        if (trends.isNotEmpty()) {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberColumnCartesianLayer(
                                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                            rememberLineComponent(color = MaterialTheme.colorScheme.primary, thickness = 10.dp, shape = Shape.rounded(50))
                                        )
                                    ),
                                    startAxis = rememberStartAxis(
                                        label = rememberTextComponent(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        tick = null,
                                        guideline = rememberLineComponent(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        label = rememberTextComponent(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        valueFormatter = { value, _, _ -> trends.getOrNull(value.toInt())?.label ?: "" }
                                    )
                                ),
                                modelProducer = columnModelProducer,
                                modifier = Modifier.height(180.dp)
                            )
                        } else {
                            Box(Modifier.height(180.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No performance data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = when (selectedFilter) {
                DateFilter.DAY -> currentWeekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DateFilter.WEEK -> currentWeekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DateFilter.MONTH -> currentWeekStart.withDayOfMonth(15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DateFilter.YEAR -> currentWeekStart.withDayOfYear(180).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        currentWeekStart = when (selectedFilter) {
                            DateFilter.DAY -> selected
                            DateFilter.WEEK -> selected.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
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
                    Text(when (selectedFilter) {
                        DateFilter.DAY -> "Select Date"
                        DateFilter.WEEK -> "Select Week"
                        DateFilter.MONTH -> "Select Month"
                        DateFilter.YEAR -> "Select Year"
                    })
                }
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryColumn(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
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
