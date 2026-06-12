package com.example.expncetracker.exptkr.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiModel(
    val category: Category,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val progress: Float
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetDao,
    private val getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    private val _budgets = budgetDao.getAllBudgets()
    private val _summary = getSummaryUseCase(DateFilter.MONTH)

    val budgetList: StateFlow<List<BudgetUiModel>> = combine(_budgets, _summary) { budgets, summary ->
        budgets.map { budget ->
            val category = Category.entries.find { it.name == budget.category } ?: Category.OTHERS
            val spent = summary.categoryDistribution[category] ?: 0.0
            val remaining = (budget.limitAmount - spent).coerceAtLeast(0.0)
            val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
            
            BudgetUiModel(category, budget.limitAmount, spent, remaining, progress)
        }
    }.catch { emptyList<BudgetUiModel>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(category: Category, limit: Double) {
        viewModelScope.launch {
            budgetDao.insertBudget(BudgetEntity(category.name, limit))
        }
    }

    fun deleteBudget(category: Category) {
        viewModelScope.launch {
            budgetDao.deleteBudget(BudgetEntity(category.name, 0.0))
        }
    }
}
