package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetDailyTotalsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, BigDecimal>> {
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return repository.getTransactionsInRange(startMillis, endMillis).map { transactions ->
            transactions
                .filter { !it.isRecurring && it.type == TransactionType.DEBIT }
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) } }
        }
    }
}
