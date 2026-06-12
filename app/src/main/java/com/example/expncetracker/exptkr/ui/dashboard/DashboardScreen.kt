package com.example.expncetracker.exptkr.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.transactions.TransactionListItem
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.theme.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToStatementLedger: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncTransactions()
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
                Column {
                    if (isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    DashboardContent(
                        summary = state.summary,
                        recent = state.recentTransactions,
                        onNavigateToAddTransaction = onNavigateToAddTransaction,
                        onSeeAllClick = onNavigateToStatementLedger,
                        onFilterChange = { viewModel.setFilter(it) }
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
    onNavigateToAddTransaction: () -> Unit,
    onSeeAllClick: () -> Unit,
    onFilterChange: (DateFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            val calendar = Calendar.getInstance()
            val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good Morning"
                in 12..15 -> "Good Afternoon"
                else -> "Good Evening"
            }
            Column {
                Text(
                    text = "$greeting!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Welcome back to MoneyWise",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            CompactSummaryHeader(summary = summary, onFilterChange = onFilterChange)
        }

        item {
            DistributionSection(summary.categoryDistribution)
        }

        item {
            Text(
                text = "Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(summary.categoryDistribution) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(summary.categoryDistribution.values.map { it.toFloat() }.ifEmpty { listOf(0f) })
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                CartesianChartHost(
                    chart = rememberCartesianChart(rememberLineCartesianLayer()),
                    modelProducer = modelProducer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        item {
            SectionHeader(
                title = "Recent Activity",
                actionLabel = "See All Ledger",
                onActionClick = onSeeAllClick
            )
        }

        if (recent.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions yet",
                    description = "Start by adding a transaction manually or syncing your SMS",
                    action = {
                        Button(
                            onClick = onNavigateToAddTransaction,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add First Transaction")
                        }
                    }
                )
            }
        } else {
            val grouped = recent.groupBy { 
                it.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, EEEE", Locale.getDefault()))
            }
            
            grouped.forEach { (date, transactions) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                
                items(transactions, key = { it.id }) { transaction ->
                    TransactionListItem(transaction)
                    if (transaction != transactions.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CompactSummaryHeader(
    summary: FinancialSummary,
    onFilterChange: (DateFilter) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val monthYearFormat = remember<SimpleDateFormat> { SimpleDateFormat("MMMM, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = monthYearFormat.format(calendar.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryColumn("EXPENSE", summary.totalExpense.formatAsCurrency(), Color(0xFFEF4444))
                SummaryColumn("INCOME", summary.totalIncome.formatAsCurrency(), Color(0xFF10B981))
                SummaryColumn("TOTAL", summary.balance.formatAsCurrency(), Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun DistributionSection(distribution: Map<Category, Double>) {
    val isDarkTheme = MaterialTheme.isDark
    val sortedDistribution = Category.entries.map { it to (distribution[it] ?: 0.0) }
        .sortedByDescending { it.second }
    
    val maxVal = distribution.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Column {
        Text(
            text = "Spending Distribution",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            sortedDistribution.forEach { (category, amount) ->
                val barHeightFraction = (amount / maxVal).toFloat().coerceIn(0f, 1f)
                val categoryColor = getCategoryColor(category, isDarkTheme)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight().weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = categoryColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFraction)
                                .background(categoryColor)
                                .align(Alignment.BottomCenter)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = constraints.minHeight,
                                            maxWidth = constraints.maxHeight,
                                            minHeight = constraints.minWidth,
                                            maxHeight = constraints.maxWidth
                                        )
                                    )
                                    layout(placeable.height, placeable.width) {
                                        placeable.placeWithLayer(
                                            x = (placeable.height - placeable.width) / 2,
                                            y = (placeable.width - placeable.height) / 2
                                        ) {
                                            rotationZ = -90f
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = if (amount >= 1000) String.format(Locale.US, "%.1fK", amount/1000) else amount.toInt().toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = category.displayName.uppercase(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Categories",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
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
        Category.GROCERIES -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
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
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
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
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
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
