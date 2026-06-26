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
import java.math.BigDecimal
import javax.inject.Inject

class GetSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao,
    private val budgetDao: com.example.expncetracker.exptkr.data.db.dao.BudgetDao
) {
    operator fun invoke(filter: DateFilter): Flow<FinancialSummary> {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val startDateTime = when (filter) {
            DateFilter.DAY -> today.atStartOfDay()
            DateFilter.WEEK -> today.minusDays(6).atStartOfDay()
            DateFilter.WEEK_RANGE -> today.minusDays(6).atStartOfDay()
            DateFilter.MONTH -> today.withDayOfMonth(1).atStartOfDay()
            DateFilter.YEAR -> today.withDayOfYear(1).atStartOfDay()
        }

        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = if (filter == DateFilter.YEAR) {
            today.withDayOfYear(today.lengthOfYear()).atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        return invoke(startMillis, endMillis)
    }

    operator fun invoke(startMillis: Long, endMillis: Long): Flow<FinancialSummary> {
        return combine(
            repository.getTransactionsInRange(startMillis, endMillis),
            accountDao.getAllAccounts(),
            budgetDao.getAllBudgets()
        ) { txList, accounts, budgets ->
            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO
            var lent = BigDecimal.ZERO
            var borrowed = BigDecimal.ZERO
            val map = mutableMapOf<String, BigDecimal>()

            txList.filter { !it.isRecurring }.forEach { tx ->
                when (tx.type) {
                    TransactionType.CREDIT -> income = income.add(tx.amount)
                    TransactionType.DEBIT -> {
                        expense = expense.add(tx.amount)
                        map[tx.categoryName] = (map[tx.categoryName] ?: BigDecimal.ZERO).add(tx.amount)
                    }
                    TransactionType.LEND -> {
                        if (!tx.isSettled) lent = lent.add(tx.amount)
                    }
                    TransactionType.BORROW -> {
                        if (!tx.isSettled) borrowed = borrowed.add(tx.amount)
                    }
                    TransactionType.TRANSFER -> { }
                }
            }

            val totalAccountBalance = accounts.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.balance) }
            val accountAssets = accounts.filter { it.balance > BigDecimal.ZERO }.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.balance) }
            val accountLiabilities = accounts.filter { it.balance < BigDecimal.ZERO }.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.balance.abs()) }
            
            val totalAssets = accountAssets.add(lent)
            val totalLiabilities = accountLiabilities.add(borrowed)
            val netWorth = totalAccountBalance.add(lent).subtract(borrowed)
            
            val totalBudget = budgets.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.limitAmount) }

            FinancialSummary(
                totalIncome = income,
                totalExpense = expense,
                balance = income.subtract(expense),
                totalLent = lent,
                totalBorrowed = borrowed,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = netWorth,
                budget = if (totalBudget > BigDecimal.ZERO) totalBudget else null,
                categoryDistribution = map
            )
        }
    }
}
