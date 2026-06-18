package com.example.expncetracker.exptkr.ui.dashboard

import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val data: DashboardData
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

data class DashboardData(
    val summary: FinancialSummary,
    val recentTransactions: List<Transaction>,
    val recurringTransactions: List<Transaction>,
    val trends: List<SpendingTrend>,
    val distribution: Map<String, Double>,
    val allCategories: List<CategoryEntity>,
    val goals: List<GoalEntity>
)
