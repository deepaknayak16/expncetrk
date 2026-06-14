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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getTrendsUseCase: GetTrendsUseCase,
    private val getDailyTotalsUseCase: GetDailyTotalsUseCase,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase,
    categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.WEEK)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    private val _weekRange = MutableStateFlow(Pair(LocalDate.now(), LocalDate.now()))
    
    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<FinancialSummary?> = combine(_selectedFilter, _refreshTrigger) { filter, _ -> filter }
        .flatMapLatest { getSummaryUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trends: StateFlow<List<SpendingTrend>> = combine(_selectedFilter, _refreshTrigger) { filter, _ -> filter }
        .flatMapLatest { filter ->
            getTrendsUseCase(filter.toMonths())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTotals: StateFlow<Map<LocalDate, Double>> = combine(_weekRange, _refreshTrigger) { range, _ -> range }
        .flatMapLatest { range ->
            getDailyTotalsUseCase(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun setWeekRange(start: LocalDate, end: LocalDate) {
        _weekRange.value = Pair(start, end)
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // First sync SMS to ensure latest data is imported
                importSmsTransactionsUseCase.execute()
                // Then trigger a re-emission of all data flows
                _refreshTrigger.value++
                _statusEvent.send("Data refreshed")
            } catch (e: Exception) {
                _statusEvent.send("Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
