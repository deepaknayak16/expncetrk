package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.core.common.BUDGET_ALERTS_ENABLED_KEY
import com.example.expncetracker.exptkr.core.common.BUDGET_THRESHOLD_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.core.notifications.NotificationHelper
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class CheckBudgetAlertsUseCase @Inject constructor(
    private val budgetDao: BudgetDao,
    private val repository: TransactionRepository,
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context
) {
    suspend fun execute() {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[BUDGET_ALERTS_ENABLED_KEY] ?: true
        if (!enabled) return

        val threshold = prefs[BUDGET_THRESHOLD_KEY] ?: 0.9f

        val month = YearMonth.now()
        val startMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val budgets = budgetDao.getAllBudgets().first()
        val txList = repository.getTransactionsInRange(startMillis, endMillis).first()
        
        val categorySpent = txList.filter { it.type == com.example.expncetracker.exptkr.domain.model.TransactionType.DEBIT }
            .groupBy { it.categoryName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.forEach { budget ->
            val spent = categorySpent[budget.category] ?: 0.0
            if (budget.limitAmount > 0) {
                val progress = (spent / budget.limitAmount).toFloat()
                if (progress >= threshold) {
                    notificationHelper.showBudgetAlert(budget.category, (progress * 100).toInt())
                }
            }
        }
    }
}
