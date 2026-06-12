package com.example.expncetracker.exptkr.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.ui.dashboard.DistributionSection
import com.example.expncetracker.exptkr.ui.theme.*
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

private val CalculatorBg = Color(0xFFFEFBEA)
private val CalculatorGreen = Color(0xFF2D5D4E)
private val CalculatorGreenLight = Color(0xFF6E9185)
private val ExpenseRed = Color(0xFFD32F2F)
private val IncomeGreen = Color(0xFF388E3C)

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val summary by viewModel.summary.collectAsState()
    val trends by viewModel.trends.collectAsState()
    
    val lineModelProducer = remember { CartesianChartModelProducer() }
    val columnModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trends) {
        if (trends.isNotEmpty()) {
            lineModelProducer.runTransaction {
                lineSeries {
                    series(trends.map { it.amount.toFloat() })
                }
            }
            columnModelProducer.runTransaction {
                columnSeries {
                    series(trends.map { it.amount.toFloat() })
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatorBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- CURRENT CHANGE (TOP) ---
        item {
            // Date Range Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = CalculatorGreen,
                    modifier = Modifier.size(32.dp).clickable { /* Handle prev */ }
                )
                
                Text(
                    text = "Jan 03 - Jan 09",
                    color = CalculatorGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = CalculatorGreen,
                        modifier = Modifier.size(32.dp).clickable { /* Handle next */ }
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = CalculatorGreen,
                        modifier = Modifier.size(28.dp).clickable { /* Handle filter */ }
                    )
                }
            }
        }

        item {
            // Stats Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("EXPENSE", "₹${summary?.totalExpense ?: 0.0}", ExpenseRed, Modifier.weight(1f))
                StatItem("INCOME", "₹${summary?.totalIncome ?: 0.0}", IncomeGreen, Modifier.weight(1f))
                StatItem("TOTAL", "₹${summary?.balance ?: 0.0}", CalculatorGreen, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            // Expense Flow Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .border(2.dp, CalculatorGreen, RoundedCornerShape(8.dp))
                    .clickable { }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CalculatorGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("EXPENSE FLOW", color = CalculatorGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            // Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp)
            ) {
                if (trends.isNotEmpty()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = rememberStartAxis(
                                label = null,
                                tick = null,
                                guideline = rememberLineComponent(color = CalculatorGreen.copy(alpha = 0.1f))
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
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            // Calendar Week View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Jan 03 - Jan 09",
                    color = CalculatorGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                
                HorizontalDivider(color = CalculatorGreen.copy(alpha = 0.3f))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    val dates = listOf("3", "4", "5", "6", "7", "8", "9")
                    val values = listOf("-416.2", "-218.7", "-175.5", "-17.2", "-95.0", "-16.2", "-54.3")
                    
                    days.forEachIndexed { index, day ->
                        CalendarDayItem(
                            day = day,
                            date = dates[index],
                            value = values[index],
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(color = CalculatorGreen.copy(alpha = 0.3f))
            }
            Spacer(Modifier.height(32.dp))
        }

        // --- PREVIOUS CHANGES (BELOW) ---

        item {
            // Spending Distribution Section
            summary?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CalculatorGreen.copy(alpha = 0.05f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Expense Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = CalculatorGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        DistributionSection(distribution = it.categoryDistribution)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            // Monthly Trends Bar Chart Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CalculatorGreen.copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Monthly Trends",
                        style = MaterialTheme.typography.titleMedium,
                        color = CalculatorGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    if (trends.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                        rememberLineComponent(
                                            color = CalculatorGreen,
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
                            Text("No trend data available", color = CalculatorGreenLight)
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
        Text(text = label, color = CalculatorGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = amount, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarDayItem(day: String, date: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(0.5.dp, CalculatorGreen.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = day, color = CalculatorGreen, fontSize = 12.sp)
        Text(text = date, color = CalculatorGreenLight, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(text = value, color = ExpenseRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
