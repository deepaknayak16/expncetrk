package com.example.expncetracker.exptkr.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.usecase.GetDailyTotalsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getTrendsUseCase: GetTrendsUseCase,
    private val getDailyTotalsUseCase: GetDailyTotalsUseCase,
    categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _weekRange = MutableStateFlow(Pair(LocalDate.now(), LocalDate.now()))
    
    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<FinancialSummary?> = _selectedFilter
        .flatMapLatest { getSummaryUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trends: StateFlow<List<SpendingTrend>> = _selectedFilter.flatMapLatest { filter ->
        getTrendsUseCase(filter.toMonths())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTotals: StateFlow<Map<LocalDate, Double>> = _weekRange.flatMapLatest { range ->
        getDailyTotalsUseCase(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun setWeekRange(start: LocalDate, end: LocalDate) {
        _weekRange.value = Pair(start, end)
    }
}
