package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
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
            val totalTrends = mutableMapOf<String, BigDecimal>()
            val categoryTrends = mutableMapOf<String, MutableMap<String, BigDecimal>>()
            
            if (days <= 30) {
                // Day-based for 1W/1M - Start from Past and move to Today (Left to Right)
                for (i in days downTo 0) {
                    val date = now.minusDays(i.toLong()).toLocalDate()
                    val label = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                    labels.add(label)
                    totalTrends[label] = BigDecimal.ZERO
                }
                txList.filter { !it.isRecurring }.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = tx.timestamp.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                        if (totalTrends.containsKey(label)) {
                            totalTrends[label] = totalTrends[label]!!.add(tx.amount)
                            
                            val catMap = categoryTrends.getOrPut(tx.categoryName) { labels.associateWith { BigDecimal.ZERO }.toMutableMap() }
                            catMap[label] = (catMap[label] ?: BigDecimal.ZERO).add(tx.amount)
                        }
                    }
                }
            } else {
                // Month-based - Start from Past Month and move to Current (Left to Right)
                val oldestMonth = startDateTime.toLocalDate().withDayOfMonth(1)
                val thisMonth = now.toLocalDate().withDayOfMonth(1)
                var currentMonthIter = oldestMonth
                
                while (!currentMonthIter.isAfter(thisMonth)) {
                    val label = "${currentMonthIter.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${currentMonthIter.year % 100}"
                    labels.add(label)
                    totalTrends[label] = BigDecimal.ZERO
                    currentMonthIter = currentMonthIter.plusMonths(1)
                }

                txList.filter { !it.isRecurring }.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = "${tx.timestamp.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${tx.timestamp.year % 100}"
                        if (totalTrends.containsKey(label)) {
                            totalTrends[label] = totalTrends[label]!!.add(tx.amount)
                            
                            val catMap = categoryTrends.getOrPut(tx.categoryName) { labels.associateWith { BigDecimal.ZERO }.toMutableMap() }
                            catMap[label] = (catMap[label] ?: BigDecimal.ZERO).add(tx.amount)
                        }
                    }
                }
            }

            val result = mutableMapOf<String, List<SpendingTrend>>()
            
            // Add Total
            result["Total"] = labels.map { SpendingTrend(it, totalTrends[it] ?: BigDecimal.ZERO) }
            
            // Add top 4 categories by total volume
            categoryTrends.entries
                .sortedByDescending { it.value.values.fold(BigDecimal.ZERO) { acc, b -> acc.add(b) } }
                .take(4)
                .forEach { (cat, trends) ->
                    result[cat] = labels.map { SpendingTrend(it, trends[it] ?: BigDecimal.ZERO) }
                }

            result
        }
    }
}
