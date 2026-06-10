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
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightBackground
import com.example.expncetracker.exptkr.ui.theme.LightBorder
import com.example.expncetracker.exptkr.ui.theme.LightCardBackground
import com.example.expncetracker.exptkr.ui.theme.LightPrimary
import com.example.expncetracker.exptkr.ui.theme.LightSecondary
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    // Check permissions before attempting sync
    LaunchedEffect(Unit) { 
        android.util.Log.d("DashboardScreen", "Attempting to sync transactions")
        viewModel.syncTransactions() 
    }

    Box(modifier = Modifier.fillMaxSize().background(LightBackground)) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = LightPrimary)
            is DashboardUiState.Error -> Text("Error: ${state.message}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            is DashboardUiState.Success -> {
                Column {
                    if (isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = LightPrimary,
                            trackColor = Color.Transparent
                        )
                    }
                    DashboardContent(state.summary, state.recentTransactions, currentFilter) { viewModel.setFilter(it) }
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
    onFilterChange: (DateFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MainDashboardCard(summary)
        }

        item {
            TimeFilterRow(currentFilter, onFilterChange)
        }

        item {
            RecentTransactionsHeader()
        }

        if (recent.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(recent, key = { it.id }) { transaction ->
                TransactionItemImproved(transaction)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MainDashboardCard(summary: FinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = LightBorder),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(LightPrimary, LightSecondary)))
                .padding(24.dp)
        ) {
            Column {
                Text("Total Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(summary.balance.formatAsCurrency(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem(label = "Income", amount = summary.totalIncome, icon = Icons.Default.ArrowDownward, color = Color(0xFF10B981))
                    SummaryItem(label = "Expenses", amount = summary.totalExpense, icon = Icons.Default.ArrowUpward, color = Color(0xFFEF4444))
                }

                if (summary.categoryDistribution.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    DistributionSection(summary.categoryDistribution)
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(amount.formatAsCurrency(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DistributionSection(distribution: Map<Category, Double>) {
    val total = distribution.values.sum()
    val colors = listOf(Color(0xFFFFB74D), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFBA68C8), Color(0xFF4DB6AC))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(60.dp)) {
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
        Column {
            distribution.entries.take(3).forEachIndexed { idx, entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors[idx % colors.size]))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.key.displayName, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TimeFilterRow(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LightCardBackground).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DateFilter.values().forEach { filter ->
            val isSelected = currentFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) LightPrimary else Color.Transparent)
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.title,
                    color = if (isSelected) Color.White else LightTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RecentTransactionsHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Recent Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)
        Text("See All", color = LightPrimary, fontSize = 14.sp, modifier = Modifier.clickable { })
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = LightTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No transactions yet", color = LightTextSecondary, fontSize = 16.sp)
        Text("Try loading sample data or syncing SMS", color = LightTextSecondary.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
