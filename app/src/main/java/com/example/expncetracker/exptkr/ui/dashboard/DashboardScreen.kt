package com.example.expncetracker.exptkr.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun DashboardScreen(viewModel: DashboardViewModel) {
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
                        state.summary,
                        state.recentTransactions,
                        currentFilter,
                        gradientStart,
                        gradientEnd
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
                    icon = Icons.Default.ReceiptLong,
                    title = "No transactions yet",
                    description = "Start by adding a transaction or load demo data"
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
    GradientCard(
        title = "Total Balance",
        value = summary.balance.formatAsCurrency(),
        subtitle = "Updated just now",
        icon = Icons.Default.AccountBalanceWallet,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatItem(
                label = "Income",
                amount = summary.totalIncome.formatAsCurrency(),
                icon = Icons.Default.ArrowDownward,
                iconBackgroundColor = LightIncome
            )
            StatItem(
                label = "Expenses",
                amount = summary.totalExpense.formatAsCurrency(),
                icon = Icons.Default.ArrowUpward,
                iconBackgroundColor = LightExpense
            )
        }
        
        if (summary.categoryDistribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            DistributionSection(summary.categoryDistribution)
        }
    }
}

@Composable
fun DistributionSection(distribution: Map<Category, Double>) {
    val isDarkTheme = MaterialTheme.isDark
    val total = distribution.values.sum()
    val colors = if (isDarkTheme) {
        listOf(CategoryFoodDark, CategoryCabsDark, CategoryBillsDark, CategoryShoppingDark, CategoryTravelDark)
    } else {
        listOf(CategoryFood, CategoryCabs, CategoryBills, CategoryShopping, CategoryTravel)
    }
    
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
                distribution.values.take(5).forEachIndexed { idx, value ->
                    val sweepAngle = (value / total * 360f).toFloat()
                    drawArc(
                        color = colors[idx % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                distribution.entries.take(3).forEachIndexed { idx, entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors[idx % colors.size])
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
            }
        }
    }
}

@Composable
fun TimeFilterRow(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DateFilter.values().forEach { filter ->
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
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
