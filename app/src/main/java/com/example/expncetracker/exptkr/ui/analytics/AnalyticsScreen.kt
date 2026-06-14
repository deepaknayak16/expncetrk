package com.example.expncetracker.exptkr.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.ui.dashboard.DistributionSection
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape
import java.time.LocalDate
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
    val isDark = MaterialTheme.isDark
    var showFilterSheet by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    var currentWeekStart by remember {
        mutableStateOf(
            LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
        )
    }
    val weekEnd = currentWeekStart.plusDays(6)

    LaunchedEffect(currentWeekStart) {
        viewModel.setWeekRange(currentWeekStart, weekEnd)
    }

    val lineModelProducer = remember { CartesianChartModelProducer() }
    val columnModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trends) {
        if (trends.isNotEmpty()) {
            lineModelProducer.runTransaction {
                lineSeries { series(trends.map { it.amount.toFloat() }) }
            }
            columnModelProducer.runTransaction {
                columnSeries { series(trends.map { it.amount.toFloat() }) }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (trends.isEmpty() && (summary == null || summary!!.categoryDistribution.isEmpty())) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.Analytics,
                    title = "No data to analyze",
                    description = "Add some transactions first to see insights and trends here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    val locale = remember { Locale.getDefault() }
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", locale) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = currentWeekStart.format(dateFormatter) + " - " + weekEnd.format(dateFormatter),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

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
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("EXPENSE", (summary?.totalExpense ?: 0.0).formatAsCurrency(), if (isDark) DarkExpense else LightExpense, Modifier.weight(1f))
                        StatItem("INCOME", (summary?.totalIncome ?: 0.0).formatAsCurrency(), if (isDark) DarkIncome else LightIncome, Modifier.weight(1f))
                        StatItem("TOTAL", (summary?.balance ?: 0.0).formatAsCurrency(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (trends.isNotEmpty()) {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberLineCartesianLayer(),
                                    startAxis = rememberStartAxis(
                                        label = null,
                                        tick = null,
                                        guideline = rememberLineComponent(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        label = null,
                                        tick = null,
                                        guideline = null,
                                        valueFormatter = { value, _, _ ->
                                            trends.getOrNull(value.toInt())?.label ?: ""
                                        }
                                    )
                                ),
                                modelProducer = lineModelProducer,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No trend data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (selectedFilter == DateFilter.WEEK) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Weekly Overview",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            val locale = remember { Locale.getDefault() }
                            val weekDays = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
                            val daySpending = weekDays.map { day ->
                                val dayTotal = dailyTotals[day] ?: 0.0
                                String.format(Locale.US, "-%.2f", dayTotal)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                weekDays.forEachIndexed { index, day ->
                                    CalendarDayItem(
                                        day = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                                        date = day.dayOfMonth.toString(),
                                        value = daySpending[index],
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }

                item {
                    summary?.let {
                        if (it.categoryDistribution.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(0.dp)) {
                                    DistributionSection(
                                        distribution = it.categoryDistribution,
                                        allCategories = allCategories
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Monthly Trends",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))

                            if (trends.isNotEmpty()) {
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        rememberColumnCartesianLayer(
                                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                                rememberLineComponent(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    thickness = 16.dp,
                                                    shape = Shape.rounded(4)
                                                )
                                            )
                                        ),
                                        startAxis = rememberStartAxis(),
                                        bottomAxis = rememberBottomAxis(
                                            valueFormatter = { value, _, _ ->
                                                trends.getOrNull(value.toInt())?.label ?: ""
                                            }
                                        )
                                    ),
                                    modelProducer = columnModelProducer,
                                    modifier = Modifier.height(200.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.height(200.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No trend data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(text = amount, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarDayItem(day: String, date: String, value: String, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.isDark
    Column(
        modifier = modifier
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = day, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
        Text(text = date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = value, color = if (isDark) DarkExpense else LightExpense, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}
