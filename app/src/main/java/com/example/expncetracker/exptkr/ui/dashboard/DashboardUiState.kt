package com.example.expncetracker.exptkr.ui.dashboard

import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.FinancialSummary

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val summary: FinancialSummary,
        val recentTransactions: List<Transaction>,
        val categories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
