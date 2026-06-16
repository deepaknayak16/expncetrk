package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.domain.model.DateFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class GetSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao
) {
    operator fun invoke(filter: DateFilter): Flow<FinancialSummary> {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val startDateTime = when (filter) {
            DateFilter.DAY -> today.atStartOfDay()
            DateFilter.WEEK -> today.minusDays(6).atStartOfDay()
            DateFilter.MONTH -> today.withDayOfMonth(1).atStartOfDay()
            DateFilter.YEAR -> today.withDayOfYear(1).atStartOfDay()
        }

        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return combine(
            repository.getTransactionsInRange(startMillis, endMillis),
            accountDao.getAllAccounts()
        ) { txList, accounts ->
            var income = 0.0
            var expense = 0.0
            var lent = 0.0
            var borrowed = 0.0
            val map = mutableMapOf<String, Double>()

            txList.forEach { tx ->
                when (tx.type) {
                    TransactionType.CREDIT -> income += tx.amount
                    TransactionType.DEBIT -> {
                        expense += tx.amount
                        map[tx.categoryName] = (map[tx.categoryName] ?: 0.0) + tx.amount
                    }
                    TransactionType.LEND -> {
                        if (!tx.isSettled) lent += tx.amount
                        // LENDING is money going out, it reflects in account balance already
                    }
                    TransactionType.BORROW -> {
                        if (!tx.isSettled) borrowed += tx.amount
                        // BORROWING is money coming in, it reflects in account balance already
                    }
                    TransactionType.TRANSFER -> { }
                }
            }

            val totalAccountBalance = accounts.sumOf { it.balance }
            val accountAssets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
            val accountLiabilities = accounts.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
            
            val totalAssets = accountAssets + lent
            val totalLiabilities = accountLiabilities + borrowed
            val netWorth = totalAccountBalance + lent - borrowed

            FinancialSummary(
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense,
                totalLent = lent,
                totalBorrowed = borrowed,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = netWorth,
                categoryDistribution = map
            )
        }
    }
}
