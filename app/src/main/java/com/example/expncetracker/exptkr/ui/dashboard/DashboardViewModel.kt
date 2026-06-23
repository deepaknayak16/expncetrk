package com.example.expncetracker.exptkr.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.domain.usecase.GetRecentTransactionsUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.usecase.GetTrendsUseCase
import com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSummaryUseCase: GetSummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val importSmsTransactionsUseCase: ImportSmsTransactionsUseCase,
    private val getTrendsUseCase: GetTrendsUseCase,
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDateTime.now())
    val selectedDate: StateFlow<LocalDateTime> = _selectedDate.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    val trends: StateFlow<Map<String, List<SpendingTrend>>> = _selectedFilter.flatMapLatest { filter ->
        val rangeDays = when (filter) {
            DateFilter.DAY, DateFilter.WEEK -> filter.toDays()
            else -> 30
        }
        getTrendsUseCase(rangeDays)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedFilter,
        _selectedDate
    ) { filter, date -> filter to date }
        .flatMapLatest { (filter, date) ->
            val startMillis: Long
            val endMillis: Long
            val zone = ZoneId.systemDefault()

            when (filter) {
                DateFilter.DAY -> {
                    startMillis = date.withHour(0).withMinute(0).withSecond(0).atZone(zone).toInstant().toEpochMilli()
                    endMillis = date.withHour(23).withMinute(59).withSecond(59).atZone(zone).toInstant().toEpochMilli()
                }
                DateFilter.WEEK -> {
                    val start = date.minusDays(6).withHour(0).withMinute(0)
                    startMillis = start.atZone(zone).toInstant().toEpochMilli()
                    endMillis = date.withHour(23).withMinute(59).withSecond(59).atZone(zone).toInstant().toEpochMilli()
                }
                DateFilter.MONTH -> {
                    val start = date.withDayOfMonth(1).withHour(0).withMinute(0)
                    val end = date.withDayOfMonth(date.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                    startMillis = start.atZone(zone).toInstant().toEpochMilli()
                    endMillis = end.atZone(zone).toInstant().toEpochMilli()
                }
                DateFilter.YEAR -> {
                    val start = date.withDayOfYear(1).withHour(0).withMinute(0)
                    val end = date.withDayOfYear(date.toLocalDate().lengthOfYear()).withHour(23).withMinute(59)
                    startMillis = start.atZone(zone).toInstant().toEpochMilli()
                    endMillis = end.atZone(zone).toInstant().toEpochMilli()
                }
                else -> {
                    startMillis = date.withHour(0).atZone(zone).toInstant().toEpochMilli()
                    endMillis = date.atZone(zone).toInstant().toEpochMilli()
                }
            }
            getSummaryUseCase(startMillis, endMillis)
        }
        .distinctUntilChanged()
        .combine(getRecentTransactionsUseCase(10).distinctUntilChanged()) { summary, recent -> summary to recent }
        .combine(repository.getAllRecurringTransactions().distinctUntilChanged()) { (summary, recent), recurring -> Triple(summary, recent, recurring) }
        .combine(categoryRepository.getAllCategories().distinctUntilChanged()) { triple, categories -> triple to categories }
        .combine(goalRepository.getAllGoals().distinctUntilChanged()) { pair, goals -> pair to goals }
        .combine(trends) { pair, trendMap ->
            val (summaryRecentRecurring, categories) = pair.first
            val (summary, recent, recurring) = summaryRecentRecurring
            val goals = pair.second

            val distribution = recent.groupBy { it.categoryName }
                .mapValues { it.value.sumOf { t -> t.amount } }

            DashboardUiState.Success(
                DashboardData(
                    summary = summary,
                    recentTransactions = recent,
                    recurringTransactions = recurring,
                    trends = trendMap.getOrDefault("Total", emptyList()),
                    distribution = distribution,
                    allCategories = categories,
                    goals = goals
                )
            ) as DashboardUiState
        }
    .catch { e ->
        emit(DashboardUiState.Error(e.message ?: "An unexpected error occurred"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

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

    fun updateDate(newDate: LocalDateTime) {
        _selectedDate.value = newDate
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                if (transaction.smsId != null) {
                    _statusEvent.send("SMS transactions cannot be deleted")
                    return@launch
                }

                val now = LocalDateTime.now()
                val minutesSinceCreation = java.time.Duration.between(transaction.createdAt, now).toMinutes()

                if (minutesSinceCreation > 60) {
                    _statusEvent.send("Manual transactions older than 1 hour cannot be deleted")
                    return@launch
                }

                repository.deleteTransactionById(transaction.id)
                _statusEvent.send("Transaction deleted")
            } catch (e: Exception) {
                _statusEvent.send("Error deleting: ${e.message}")
            }
        }
    }

    fun settleTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.settleTransaction(transaction)
                _statusEvent.send("Settled successfully")
            } catch (e: Exception) {
                _statusEvent.send("Settlement failed: ${e.message}")
            }
        }
    }
}
