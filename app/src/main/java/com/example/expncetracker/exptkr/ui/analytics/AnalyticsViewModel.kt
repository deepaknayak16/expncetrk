package com.example.expncetracker.exptkr.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.usecase.GetDailyTotalsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getTrendsUseCase: GetTrendsUseCase,
    private val getDailyTotalsUseCase: GetDailyTotalsUseCase,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase,
    private val categoryDao: CategoryDao,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.WEEK)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent: Flow<String> = _statusEvent.receiveAsFlow()

    private val _weekRange = MutableStateFlow(Pair(
        LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L),
        LocalDate.now().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L).plusDays(6)
    ))

    private val _prevRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    
    private val _trendDays = MutableStateFlow(30)

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<FinancialSummary?> = combine(_weekRange, _refreshTrigger) { range, _ -> range }
        .flatMapLatest { range ->
            val startMillis = range.first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = range.second.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            getSummaryUseCase(startMillis, endMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val previousSummary: StateFlow<FinancialSummary?> = combine(_prevRange, _refreshTrigger) { range, _ -> range }
        .flatMapLatest { range ->
            if (range == null) flowOf(null)
            else {
                val startMillis = range.first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = range.second.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                getSummaryUseCase(startMillis, endMillis)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trends: StateFlow<Map<String, List<SpendingTrend>>> = combine(_trendDays, _refreshTrigger) { days, _ -> days }
        .flatMapLatest { days ->
            getTrendsUseCase(days)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dailyTotals: StateFlow<Map<LocalDate, Double>> = combine(_weekRange, _refreshTrigger) { range, _ -> range }
        .flatMapLatest { range ->
            getDailyTotalsUseCase(range.first, range.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentTransactions: StateFlow<List<Transaction>> = combine(_weekRange, _refreshTrigger) { range, _ -> range }
        .flatMapLatest { range ->
            val startMillis = range.first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = range.second.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            transactionRepository.getTransactionsInRange(startMillis, endMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
        // Reset range to current based on filter
        val today = LocalDate.now()
        _weekRange.value = when (filter) {
            DateFilter.DAY -> Pair(today, today)
            DateFilter.WEEK -> Pair(
                today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L),
                today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L).plusDays(6)
            )
            DateFilter.WEEK_RANGE -> _weekRange.value // Keep existing if switching back
            DateFilter.MONTH -> Pair(
                today.withDayOfMonth(1),
                today.withDayOfMonth(today.lengthOfMonth())
            )
            DateFilter.YEAR -> Pair(
                today.withDayOfYear(1),
                today.withDayOfYear(today.lengthOfYear())
            )
        }
    }

    fun setWeekRange(start: LocalDate, end: LocalDate) {
        _weekRange.value = Pair(start, end)
        // Removed: _selectedFilter.value = DateFilter.WEEK_RANGE
    }

    fun setCustomRange(startMillis: Long, endMillis: Long) {
        val start = java.time.Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val end = java.time.Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        setWeekRange(start, end)
    }

    fun loadPreviousPeriod(currentStart: LocalDate, filter: DateFilter) {
        val range = when (filter) {
            DateFilter.DAY -> {
                val d = currentStart.minusDays(1)
                Pair(d, d)
            }
            DateFilter.WEEK -> {
                val s = currentStart.minusWeeks(1)
                Pair(s, s.plusDays(6))
            }
            DateFilter.WEEK_RANGE -> {
                val s = currentStart.minusWeeks(1)
                Pair(s, s.plusDays(6))
            }
            DateFilter.MONTH -> {
                val s = currentStart.minusMonths(1).withDayOfMonth(1)
                Pair(s, s.withDayOfMonth(s.lengthOfMonth()))
            }
            DateFilter.YEAR -> {
                val s = currentStart.minusYears(1).withDayOfYear(1)
                Pair(s, s.withDayOfYear(s.lengthOfYear()))
            }
        }
        _prevRange.value = range
    }

    fun loadTrends(days: Int) {
        _trendDays.value = days
    }

    fun getTransactionsForCategory(categoryName: String): List<Transaction> {
        return currentTransactions.value.filter { it.categoryName == categoryName }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
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
