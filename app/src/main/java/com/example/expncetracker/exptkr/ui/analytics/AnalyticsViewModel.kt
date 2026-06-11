package com.example.expncetracker.exptkr.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getTrendsUseCase: GetTrendsUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    val summary: StateFlow<FinancialSummary?> = _selectedFilter
        .flatMapLatest { getSummaryUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trends: StateFlow<List<SpendingTrend>> = getTrendsUseCase(6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }
}
