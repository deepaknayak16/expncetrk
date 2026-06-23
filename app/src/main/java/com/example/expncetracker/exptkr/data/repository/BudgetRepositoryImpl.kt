package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    override suspend fun insertBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)
    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)
    override suspend fun getBudgetByCategory(categoryName: String): BudgetEntity? = budgetDao.getBudgetByCategory(categoryName)
    override suspend fun clearAll() = budgetDao.clearAll()
}
