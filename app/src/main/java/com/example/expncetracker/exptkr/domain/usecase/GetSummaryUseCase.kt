package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class GetSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(filter: DateFilter): Flow<FinancialSummary> {
        val now = LocalDateTime.now()
        val startDateTime = when (filter) {
            DateFilter.DAY -> now.withHour(0).withMinute(0).withSecond(0)
            DateFilter.WEEK -> now.minusDays(7)
            DateFilter.MONTH -> now.withDayOfMonth(1).withHour(0).withMinute(0)
            DateFilter.YEAR -> now.withDayOfYear(1).withHour(0).withMinute(0)
        }

        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return repository.getTransactionsInRange(startMillis, endMillis).map { txList ->
            var income = 0.0
            var expense = 0.0
            val map = mutableMapOf<String, Double>()

            txList.forEach { tx ->
                if (tx.type == TransactionType.CREDIT) {
                    income += tx.amount
                } else {
                    expense += tx.amount
                    map[tx.categoryName] = (map[tx.categoryName] ?: 0.0) + tx.amount
                }
            }
            FinancialSummary(income, expense, income - expense, map)
        }
    }
}
