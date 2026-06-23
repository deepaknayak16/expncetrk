package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    suspend fun insertBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun getBudgetByCategory(categoryName: String): BudgetEntity?
    suspend fun clearAll()
}
