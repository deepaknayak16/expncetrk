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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
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
    private val getSummaryUseCase: GetSummaryUseCase,
    private val repository: com.example.expncetracker.exptkr.domain.repository.TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _budgets = budgetDao.getAllBudgets()
    
    val budgetList: StateFlow<List<BudgetUiModel>> = combine(_budgets, _selectedMonth) { budgets, month ->
        // Fetch summary for the specific month
        val startMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // This is a bit heavy for a combine, but better for now than hardcoded month
        // In a real app, this should be a dedicated usecase or flow
        val txList = repository.getTransactionsInRange(startMillis, endMillis).first()
        val categorySpent = txList.filter { it.type == com.example.expncetracker.exptkr.domain.model.TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.map { budget ->
            val category = Category.entries.find { it.name == budget.category } ?: Category.OTHERS
            val spent = categorySpent[category] ?: 0.0
            val remaining = (budget.limitAmount - spent).coerceAtLeast(0.0)
            val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
            
            BudgetUiModel(category, budget.limitAmount, spent, remaining, progress)
        }
    }.catch { emptyList<BudgetUiModel>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

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
