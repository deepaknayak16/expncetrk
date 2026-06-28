package com.example.expncetracker.exptkr.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.RecurringTemplateDao
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.SpendingTrend
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.RecurringState
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
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
    private val goalRepository: GoalRepository,
    private val recurringTemplateDao: RecurringTemplateDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDateTime.now())
    val selectedDate: StateFlow<LocalDateTime> = _selectedDate.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val trends: StateFlow<Map<String, List<SpendingTrend>>> = combine(_selectedFilter, _refreshTrigger) { filter, _ -> filter }
        .flatMapLatest { filter ->
            val rangeDays = when (filter) {
                DateFilter.DAY, DateFilter.WEEK -> filter.toDays()
                else -> 30
            }
            getTrendsUseCase(rangeDays)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedFilter,
        _selectedDate,
        _refreshTrigger
    ) { filter, date, _ -> filter to date }
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

            val prevSummaryFlow = if (filter == DateFilter.WEEK_RANGE) flowOf(null) 
            else {
                val (pStart, pEnd) = when (filter) {
                    DateFilter.DAY -> {
                        val d = date.minusDays(1)
                        d.withHour(0).withMinute(0).atZone(zone).toInstant().toEpochMilli() to 
                        d.withHour(23).withMinute(59).atZone(zone).toInstant().toEpochMilli()
                    }
                    DateFilter.WEEK -> {
                        val dEnd = date.minusDays(7)
                        val dStart = dEnd.minusDays(6)
                        dStart.withHour(0).withMinute(0).atZone(zone).toInstant().toEpochMilli() to
                        dEnd.withHour(23).withMinute(59).atZone(zone).toInstant().toEpochMilli()
                    }
                    DateFilter.MONTH -> {
                        val d = date.minusMonths(1)
                        d.withDayOfMonth(1).withHour(0).withMinute(0).atZone(zone).toInstant().toEpochMilli() to
                        d.withDayOfMonth(d.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).atZone(zone).toInstant().toEpochMilli()
                    }
                    DateFilter.YEAR -> {
                        val d = date.minusYears(1)
                        d.withDayOfYear(1).withHour(0).withMinute(0).atZone(zone).toInstant().toEpochMilli() to
                        d.withDayOfYear(d.toLocalDate().lengthOfYear()).withHour(23).withMinute(59).atZone(zone).toInstant().toEpochMilli()
                    }
                    else -> 0L to 0L
                }
                if (pStart != 0L) getSummaryUseCase(pStart, pEnd).distinctUntilChanged() else flowOf(null)
            }

            combine(
                getSummaryUseCase(startMillis, endMillis).distinctUntilChanged(),
                prevSummaryFlow,
                getRecentTransactionsUseCase(10).distinctUntilChanged(),
                repository.getAllRecurringTransactions().distinctUntilChanged(),
                categoryRepository.getAllCategories().distinctUntilChanged(),
                goalRepository.getAllGoals().distinctUntilChanged(),
                recurringTemplateDao.getTemplatesByState(RecurringState.PENDING_CONFIRM.name).distinctUntilChanged(),
                repository.getTransactionsInRange(startMillis, endMillis).distinctUntilChanged(),
                trends
            ) { args: Array<Any?> ->
                val currentSummary = args[0] as FinancialSummary
                val previousSummary = args[1] as FinancialSummary?
                val recent = args[2] as List<Transaction>
                val recurring = args[3] as List<Transaction>
                val categories = args[4] as List<CategoryEntity>
                val goals = args[5] as List<GoalEntity>
                val pending = args[6] as List<RecurringTemplateEntity>
                val allTxsInRange = args[7] as List<Transaction>
                val trendMap = args[8] as Map<String, List<SpendingTrend>>
                
                val distribution = allTxsInRange
                    .filter { it.type == com.example.expncetracker.exptkr.domain.model.TransactionType.DEBIT }
                    .groupBy { it.categoryName }
                    .mapValues { it.value.sumOf { t -> t.amount } }

                DashboardUiState.Success(
                    DashboardData(
                        summary = currentSummary,
                        previousSummary = previousSummary,
                        recentTransactions = recent,
                        recurringTransactions = recurring,
                        trends = trendMap.getOrDefault("Total", emptyList()),
                        distribution = distribution,
                        allCategories = categories,
                        goals = goals,
                        pendingConfirmTemplates = pending
                    )
                ) as DashboardUiState
            }.catch { e ->
                emit(DashboardUiState.Error(e.message ?: "An unexpected error occurred"))
            }
        }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    fun syncTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                importSmsTransactionsUseCase.execute()
                _refreshTrigger.value++
            } catch (e: Exception){
                _statusEvent.send("Sync failed: ${e.localizedMessage}")
                _refreshTrigger.value++
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
                if (transaction.smsId != null || transaction.idempotencyHash != null) {
                    _statusEvent.send("Auto-detect SMS transactions cannot be deleted")
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

    fun confirmRecurring(templateId: Long, isConfirmed: Boolean) {
        viewModelScope.launch {
            val templates = (uiState.value as? DashboardUiState.Success)?.data?.pendingConfirmTemplates ?: return@launch
            val template = templates.find { it.id == templateId } ?: return@launch
            
            if (isConfirmed) {
                recurringTemplateDao.update(template.copy(state = RecurringState.ACTIVE.name))
                _statusEvent.send("Bill tracking enabled for ${template.merchantName}")
            } else {
                recurringTemplateDao.update(template.copy(state = RecurringState.CANCELLED.name))
                _statusEvent.send("Bill tracking ignored")
            }
        }
    }
}
