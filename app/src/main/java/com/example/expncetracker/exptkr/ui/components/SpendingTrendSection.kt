package com.example.expncetracker.exptkr.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill

import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import java.math.BigDecimal

@Composable
fun SpendingTrendSection(
    trends: List<SpendingTrend>,
    previousSummary: FinancialSummary? = null,
    currentSummary: FinancialSummary? = null
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val totalSpending = remember(trends) { trends.sumOf { it.amount } }
    
    // Calculate percentage change (Current period total vs Previous period total)
    val percentageChange = remember(currentSummary, previousSummary, trends) {
        val current = currentSummary?.totalExpense?.toDouble() ?: trends.sumOf { it.amount }.toDouble()
        val previous = previousSummary?.totalExpense?.toDouble() ?: 0.0

        if (previous > 0) {
            ((current - previous) / previous * 100).toInt()
        } else if (current > 0 && previousSummary != null) {
            100 // If we have a previous period but it was 0, it's a 100% increase
        } else {
            // Fallback to original logic if summaries are missing (e.g. initial load or legacy calls)
            if (trends.size >= 2) {
                val last = trends.last().amount.toDouble()
                val first = trends.first().amount.toDouble()
                if (first > 0) ((last - first) / first * 100).toInt() 
                else if (last > 0) 100
                else 0
            } else 0
        }
    }

    LaunchedEffect(trends) {
        modelProducer.runTransaction {
            lineSeries { series(trends.map { it.amount.toFloat() }) }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalSpending.formatAsCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))

                    val isIncrease = percentageChange > 0
                    val deltaColor = if (isIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    val containerColor = if (isIncrease) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                    val contentColor = if (isIncrease) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    percentageChange > 0 -> Icons.AutoMirrored.Filled.TrendingUp
                                    percentageChange < 0 -> Icons.AutoMirrored.Filled.TrendingDown
                                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = contentColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (percentageChange > 0) "+" else ""}$percentageChange%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val line = LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 3.dp),
                areaFill = LineCartesianLayer.AreaFill.single(
                    Fill(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(line)
                        )
                    ),
                    modelProducer = modelProducer,
                    scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
                    zoomState = rememberVicoZoomState(
                        initialZoom = Zoom.Content
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
