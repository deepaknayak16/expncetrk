package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class GetTrendsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(days: Int = 30): Flow<Map<String, List<SpendingTrend>>> {
        val now = LocalDateTime.now()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startDateTime = now.minusDays(days.toLong()).withHour(0).withMinute(0)
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return repository.getTransactionsInRange(startMillis, endMillis).map { txList ->
            val labels = mutableListOf<String>()
            val totalTrends = mutableMapOf<String, Double>()
            val categoryTrends = mutableMapOf<String, MutableMap<String, Double>>()
            
            if (days <= 30) {
                // Day-based for 1W/1M - Start from Today and move backward
                for (i in 0..days) {
                    val date = now.minusDays(i.toLong()).toLocalDate()
                    val label = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                    labels.add(label)
                    totalTrends[label] = 0.0
                }
                txList.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = tx.timestamp.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                        if (totalTrends.containsKey(label)) {
                            totalTrends[label] = totalTrends[label]!! + tx.amount
                            
                            val catMap = categoryTrends.getOrPut(tx.categoryName) { labels.associateWith { 0.0 }.toMutableMap() }
                            catMap[label] = (catMap[label] ?: 0.0) + tx.amount
                        }
                    }
                }
            } else {
                // Month-based - Start from Current Month and move backward
                val startMonth = startDateTime.toLocalDate().withDayOfMonth(1)
                var currentMonth = now.toLocalDate().withDayOfMonth(1)
                
                while (!currentMonth.isBefore(startMonth)) {
                    val label = "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${currentMonth.year % 100}"
                    labels.add(label)
                    totalTrends[label] = 0.0
                    currentMonth = currentMonth.minusMonths(1)
                }

                txList.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = "${tx.timestamp.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${tx.timestamp.year % 100}"
                        if (totalTrends.containsKey(label)) {
                            totalTrends[label] = totalTrends[label]!! + tx.amount
                            
                            val catMap = categoryTrends.getOrPut(tx.categoryName) { labels.associateWith { 0.0 }.toMutableMap() }
                            catMap[label] = (catMap[label] ?: 0.0) + tx.amount
                        }
                    }
                }
            }

            val result = mutableMapOf<String, List<SpendingTrend>>()
            
            // Add Total - No .reversed() call ensures index 0 (Today) is on the left
            result["Total"] = labels.map { SpendingTrend(it, totalTrends[it] ?: 0.0) }
            
            // Add top 4 categories by total volume
            categoryTrends.entries
                .sortedByDescending { it.value.values.sum() }
                .take(4)
                .forEach { (cat, trends) ->
                    result[cat] = labels.map { SpendingTrend(it, trends[it] ?: 0.0) }
                }

            result
        }
    }
}
