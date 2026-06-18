package com.example.expncetracker.exptkr.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetRecentTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.dao.GoalDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val importSmsTransactionsUseCase: ImportSmsTransactionsUseCase,
    private val getTrendsUseCase: GetTrendsUseCase,
    private val repository: TransactionRepository,
    private val categoryDao: CategoryDao,
    private val goalDao: GoalDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()
    
    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedFilter.flatMapLatest { getSummaryUseCase(it) }.distinctUntilChanged(),
        getRecentTransactionsUseCase(10).distinctUntilChanged(),
        categoryDao.getAllCategories().distinctUntilChanged(),
        repository.getAllRecurringTransactions().distinctUntilChanged(),
        goalDao.getAllGoals().distinctUntilChanged()
    ) { summary, recent, categories, recurring, goals ->
        DashboardUiState.Success(summary, recent, categories, recurring, goals) as DashboardUiState
    }
    .catch { e ->
        emit(DashboardUiState.Error(e.message ?: "An unexpected error occurred"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    val trends: StateFlow<List<SpendingTrend>> = _selectedFilter.flatMapLatest { filter ->
        getTrendsUseCase(filter.toDays())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                importSmsTransactionsUseCase.execute()
            } catch (e: Exception){
                _statusEvent.send("Sync failed: ${e.localizedMessage}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }
}
