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
    operator fun invoke(months: Int = 6): Flow<List<SpendingTrend>> {
        val now = LocalDateTime.now()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val startDateTime = now.minusMonths(months.toLong() - 1).withDayOfMonth(1).withHour(0).withMinute(0)
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return repository.getTransactionsInRange(startMillis, endMillis).map { txList ->
            val trends = mutableMapOf<String, Double>()
            
            // Initialize last N months with 0
            for (i in 0 until months) {
                val month = now.minusMonths(i.toLong())
                val label = "${month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${month.year % 100}"
                trends[label] = 0.0
            }

            txList.forEach { tx ->
                if (tx.type == TransactionType.DEBIT || tx.type == TransactionType.LEND || tx.type == TransactionType.TRANSFER) {
                    val label = "${tx.timestamp.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} '${tx.timestamp.year % 100}"
                    if (trends.containsKey(label)) {
                        trends[label] = trends[label]!! + tx.amount
                    }
                }
            }

            trends.entries.map { SpendingTrend(it.key, it.value) }.reversed()
        }
    }
}
