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
    operator fun invoke(days: Int = 30): Flow<List<SpendingTrend>> {
        val now = LocalDateTime.now()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startDateTime = now.minusDays(days.toLong()).withHour(0).withMinute(0)
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return repository.getTransactionsInRange(startMillis, endMillis).map { txList ->
            val trends = mutableMapOf<String, Double>()
            
            // For smaller ranges, group by day. For larger, group by month?
            // The user's screen seems to expect labels like "MMM 'yy" but for 1W/1M it might be different.
            // Let's stick to month-based if days > 30, otherwise maybe day-based?
            // Actually, the original implementation was month-based.
            
            if (days <= 30) {
                // Day-based for 1W/1M - Ensure we cover the full range including the start day
                for (i in 0..days) {
                    val date = now.minusDays(i.toLong()).toLocalDate()
                    val label = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                    trends[label] = 0.0
                }
                txList.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = tx.timestamp.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
                        if (trends.containsKey(label)) {
                            trends[label] = trends[label]!! + tx.amount
                        }
                    }
                }
            } else {
                // Month-based - Ensure we cover all months in the range
                val startMonth = startDateTime.toLocalDate().withDayOfMonth(1)
                var currentMonth = now.toLocalDate().withDayOfMonth(1)
                
                while (!currentMonth.isBefore(startMonth)) {
                    val label = "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${currentMonth.year % 100}"
                    trends[label] = 0.0
                    currentMonth = currentMonth.minusMonths(1)
                }

                txList.forEach { tx ->
                    if (tx.type == TransactionType.DEBIT) {
                        val label = "${tx.timestamp.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${tx.timestamp.year % 100}"
                        if (trends.containsKey(label)) {
                            trends[label] = trends[label]!! + tx.amount
                        }
                    }
                }
            }

            trends.entries.map { SpendingTrend(it.key, it.value) }.reversed()
        }
    }
}
