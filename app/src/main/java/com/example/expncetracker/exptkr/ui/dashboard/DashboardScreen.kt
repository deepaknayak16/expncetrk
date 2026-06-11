package com.example.expncetracker.exptkr.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.transactions.TransactionItemImproved
import com.example.expncetracker.exptkr.ui.components.GradientCard
import com.example.expncetracker.exptkr.ui.components.StatItem
import com.example.expncetracker.exptkr.ui.components.SectionHeader
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isDarkTheme = MaterialTheme.isDark

    // Gradient colors based on theme
    val gradientStart = if (isDarkTheme) CardGradientStartDark else CardGradientStartLight
    val gradientEnd = if (isDarkTheme) CardGradientEndDark else CardGradientEndLight

    // Check permissions before attempting sync
    LaunchedEffect(Unit) {
        android.util.Log.d("DashboardScreen", "Attempting to sync transactions")
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
                        currentFilter = currentFilter,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        onNavigateToAddTransaction = onNavigateToAddTransaction
                    ) { viewModel.setFilter(it) }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    summary: FinancialSummary,
    recent: List<Transaction>,
    currentFilter: DateFilter,
    gradientStart: Color,
    gradientEnd: Color,
    onNavigateToAddTransaction: () -> Unit,
    onFilterChange: (DateFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MainDashboardCard(summary, gradientStart, gradientEnd)
        }

        item {
            TimeFilterRow(currentFilter, onFilterChange)
        }

        item {
            SectionHeader(
                title = "Recent Activity",
                actionLabel = "See All",
                onActionClick = { /* Navigate to transactions */ }
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
            items(recent, key = { it.id }) { transaction ->
                TransactionItemImproved(transaction)
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MainDashboardCard(
    summary: FinancialSummary,
    gradientStart: Color,
    gradientEnd: Color
) {
    val animatedBalance by animateFloatAsState(
        targetValue = summary.balance.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "balanceAnimation"
    )

    GradientCard(
        title = "Total Balance",
        value = animatedBalance.toDouble().formatAsCurrency(),
        subtitle = "Updated just now",
        icon = Icons.Default.AccountBalanceWallet,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val animatedIncome by animateFloatAsState(
                targetValue = summary.totalIncome.toFloat(),
                animationSpec = tween(durationMillis = 1000),
                label = "incomeAnimation"
            )
            val animatedExpense by animateFloatAsState(
                targetValue = summary.totalExpense.toFloat(),
                animationSpec = tween(durationMillis = 1000),
                label = "expenseAnimation"
            )

            StatItem(
                label = "Income",
                amount = animatedIncome.toDouble().formatAsCurrency(),
                icon = Icons.Default.ArrowDownward,
                iconBackgroundColor = LightIncome
            )
            StatItem(
                label = "Expenses",
                amount = animatedExpense.toDouble().formatAsCurrency(),
                icon = Icons.Default.ArrowUpward,
                iconBackgroundColor = LightExpense
            )
        }

        if (summary.categoryDistribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            DistributionSection(summary.categoryDistribution)
        }
    }
}

@Composable
fun DistributionSection(distribution: Map<Category, Double>) {
    val isDarkTheme = MaterialTheme.isDark
    val sortedDistribution = distribution.entries
        .sortedByDescending { it.value }
        .take(5)
    
    val total = distribution.values.sum()

    Column {
        Text(
            text = "Spending Distribution",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(64.dp)) {
                var startAngle = -90f
                sortedDistribution.forEach { entry ->
                    val sweepAngle = ((entry.value / total) * 360f).toFloat()
                    drawArc(
                        color = getCategoryColor(entry.key, isDarkTheme),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
                
                // If there are more categories, draw the "Others" slice
                val shownTotal = sortedDistribution.sumOf { it.value }
                if (shownTotal < total) {
                    val sweepAngle = ((total - shownTotal) / total * 360f).toFloat()
                    drawArc(
                        color = if (isDarkTheme) CategoryOthersDark else CategoryOthers,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sortedDistribution.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(entry.key, isDarkTheme))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            entry.key.displayName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                val shownTotal = sortedDistribution.sumOf { it.value }
                if (shownTotal < total) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isDarkTheme) CategoryOthersDark else CategoryOthers)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Others",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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