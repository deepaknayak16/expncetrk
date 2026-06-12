package com.example.expncetracker.exptkr.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetRecentTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val importSmsTransactionsUseCase: ImportSmsTransactionsUseCase,
    private val getTrendsUseCase: GetTrendsUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedFilter.flatMapLatest { getSummaryUseCase(it) },
        getRecentTransactionsUseCase(10)
    ) { summary, recent ->
        DashboardUiState.Success(summary, recent) as DashboardUiState
    }
    .catch { e ->
        emit(DashboardUiState.Error(e.message ?: "An unexpected error occurred"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    val trends: StateFlow<List<SpendingTrend>> = _selectedFilter.flatMapLatest {
        getTrendsUseCase(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                android.util.Log.d("DashboardViewModel", "Starting SMS import...")
                importSmsTransactionsUseCase.execute()
                android.util.Log.d("DashboardViewModel", "SMS import completed")
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error during SMS import: ${e.message}", e)
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }
}
