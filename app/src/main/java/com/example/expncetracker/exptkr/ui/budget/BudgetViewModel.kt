package com.example.expncetracker.exptkr.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class BudgetUiModel(
    val categoryName: String,
    val displayName: String,
    val iconName: String,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val progress: Float
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetDao,
    private val getSummaryUseCase: GetSummaryUseCase,
    private val repository: com.example.expncetracker.exptkr.domain.repository.TransactionRepository,
    private val categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _budgets = budgetDao.getAllBudgets()
    
    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetList: StateFlow<List<BudgetUiModel>> = _selectedMonth.flatMapLatest { month ->
        val startMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        repository.getTransactionsInRange(startMillis, endMillis).combine(_budgets) { txList, budgets ->
            val categorySpent = txList.filter { it.type == com.example.expncetracker.exptkr.domain.model.TransactionType.DEBIT }
                .groupBy { it.categoryName }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            budgets.map { budget ->
                val spent = categorySpent[budget.category] ?: 0.0
                val remaining = (budget.limitAmount - spent).coerceAtLeast(0.0)
                val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                
                BudgetUiModel(
                    categoryName = budget.category,
                    displayName = budget.category,
                    iconName = budget.category,
                    limit = budget.limitAmount,
                    spent = spent,
                    remaining = remaining,
                    progress = progress
                )
            }
        }
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    fun triggerAddBudget() {
        _showAddDialog.value = true
    }

    fun onDialogDismissed() {
        _showAddDialog.value = false
    }

    fun refreshBudgets() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Triggering a refresh should probably call SMS sync or similar
                // For now, since Room Flows are reactive, we just ensure it's not a no-op if possible
                // butRoom flows handle local data changes automatically.
                delay(300) 
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun saveBudget(categoryName: String, limit: Double) {
        viewModelScope.launch {
            budgetDao.insertBudget(BudgetEntity(categoryName, limit))
        }
    }

    fun deleteBudgetByName(categoryName: String) {
        viewModelScope.launch {
            budgetDao.deleteBudget(BudgetEntity(categoryName, 0.0))
        }
    }
}
