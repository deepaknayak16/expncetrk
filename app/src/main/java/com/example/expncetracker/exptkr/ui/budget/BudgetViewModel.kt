package com.example.expncetracker.exptkr.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.domain.repository.BudgetRepository
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

import java.math.BigDecimal

data class BudgetUiModel(
    val categoryName: String,
    val displayName: String,
    val iconName: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val progress: Float
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val getSummaryUseCase: GetSummaryUseCase,
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _budgets = budgetRepository.getAllBudgets()
    
    val categories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()
    private val _refreshTrigger = MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetList: StateFlow<List<BudgetUiModel>> = combine(_selectedMonth, _refreshTrigger) { month, _ -> month }
        .flatMapLatest { month ->
        val startMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        repository.getTransactionsInRange(startMillis, endMillis).combine(_budgets) { txList, budgets ->
            val categorySpent = txList.filter { it.type == com.example.expncetracker.exptkr.domain.model.TransactionType.DEBIT }
                .groupBy { it.categoryName }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            budgets.map { budget ->
                val spent = categorySpent[budget.category] ?: BigDecimal.ZERO
                val remaining = (budget.limitAmount - spent).coerceAtLeast(BigDecimal.ZERO)
                val progress = if (budget.limitAmount > BigDecimal.ZERO) (spent.divide(budget.limitAmount, 4, java.math.RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f) else 0f
                
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
        }.flowOn(Dispatchers.Default)
        .catch { e ->
            _statusEvent.trySend("Budget load failed: ${e.message}")
            emit(emptyList())
        }
    }
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
                _refreshTrigger.value++
                delay(300)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setMonth(month: YearMonth) {
        if (month <= YearMonth.now()) {
            _selectedMonth.value = month
        }
    }

    fun saveBudget(categoryName: String, limit: Double) {
        viewModelScope.launch {
            if (categoryName.isBlank()) {
                _statusEvent.send("Please select a category")
                return@launch
            }
            if (limit <= 0) {
                _statusEvent.send("Budget limit must be greater than 0")
                return@launch
            }
            budgetRepository.insertBudget(BudgetEntity(categoryName, limit.toBigDecimal()))
        }
    }

    fun deleteBudgetByName(categoryName: String) {
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetByCategory(categoryName)
            if (budget != null) {
                budgetRepository.deleteBudget(budget)
                _statusEvent.send("Budget deleted")
            } else {
                _statusEvent.send("Budget not found")
            }
        }
    }
}
