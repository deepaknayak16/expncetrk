package com.example.expncetracker.exptkr.ui.dashboard

import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import java.math.BigDecimal

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val data: DashboardData
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

data class DashboardData(
    val summary: FinancialSummary,
    val previousSummary: FinancialSummary? = null,
    val recentTransactions: List<Transaction>,
    val recurringTransactions: List<Transaction>,
    val trends: List<SpendingTrend>,
    val distribution: Map<String, BigDecimal>,
    val allCategories: List<CategoryEntity>,
    val goals: List<GoalEntity>,
    val pendingConfirmTemplates: List<RecurringTemplateEntity> = emptyList()
)
