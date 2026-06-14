package com.example.expncetracker.exptkr.ui.analytics

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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var previousFilter by remember { mutableStateOf(selectedFilter) }
    LaunchedEffect(selectedFilter) {
        if (previousFilter != DateFilter.WEEK && selectedFilter == DateFilter.WEEK) {
            currentWeekStart = LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
        } else if (selectedFilter == DateFilter.DAY) {
            currentWeekStart = LocalDate.now()
        }
        previousFilter = selectedFilter
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Navigation & Period Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    val locale = remember { Locale.getDefault() }
                    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", locale) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = MaterialTheme.colorScheme.primary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedFilter == DateFilter.WEEK) "WEEKLY VIEW" else selectedFilter.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${currentWeekStart.format(dateFormatter)} - ${weekEnd.format(dateFormatter)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row {
                            IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.FilterList, "Filter", tint = MaterialTheme.colorScheme.primary)
                            }
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

            // 2. Summary Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryItem("EXPENSE", (summary?.totalExpense ?: 0.0).formatAsCurrency(), if (isDark) DarkExpense else LightExpense)
                        SummaryItem("INCOME", (summary?.totalIncome ?: 0.0).formatAsCurrency(), if (isDark) DarkIncome else LightIncome)
                    }
                }
            }

            // 3. Trends Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Growth Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            if (trends.isNotEmpty()) {
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        rememberLineCartesianLayer(),
                                        startAxis = rememberStartAxis(
                                            label = null, tick = null,
                                            guideline = rememberLineComponent(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ),
                                        bottomAxis = rememberBottomAxis(
                                            label = null, tick = null, guideline = null,
                                            valueFormatter = { value, _, _ -> trends.getOrNull(value.toInt())?.label ?: "" }
                                        )
                                    ),
                                    modelProducer = lineModelProducer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // 4. Weekly Breakdown (The "Weekly Thing")
            if (selectedFilter == DateFilter.WEEK) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Daily Spending",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        val locale = remember { Locale.getDefault() }
                        val weekDays = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                weekDays.forEach { day ->
                                    val amount = dailyTotals[day] ?: 0.0
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = day.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (amount > 0) MaterialTheme.colorScheme.error else Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Distribution Card
            item {
                summary?.let {
                    if (it.categoryDistribution.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            DistributionSection(
                                distribution = it.categoryDistribution,
                                allCategories = allCategories
                            )
                        }
                    }
                }
            }

            // 6. Monthly Column Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Monthly Performance",
                            style = MaterialTheme.typography.titleMedium,
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
                                                thickness = 12.dp,
                                                shape = Shape.rounded(40)
                                            )
                                        )
                                    ),
                                    startAxis = rememberStartAxis(
                                        label = null, tick = null,
                                        guideline = rememberLineComponent(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        valueFormatter = { value, _, _ -> trends.getOrNull(value.toInt())?.label ?: "" }
                                    )
                                ),
                                modelProducer = columnModelProducer,
                                modifier = Modifier.height(180.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
    }
}
