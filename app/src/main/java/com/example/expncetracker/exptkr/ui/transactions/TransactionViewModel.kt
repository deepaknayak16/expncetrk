package com.example.expncetracker.exptkr.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

enum class SortOrder(val title: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase,
    categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {
    
    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _statusEvent = Channel<String>()
    val statusEvent = _statusEvent.receiveAsFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = combine(
        _selectedFilter,
        _searchQuery.debounce(300L),
        _sortOrder
    ) { filter, query, sort ->
        Triple(filter, query, sort)
    }.flatMapLatest { (filter, query, sort) ->
        _isLoading.value = true
        val now = LocalDateTime.now()
        val startDateTime = when (filter) {
            DateFilter.DAY -> now.withHour(0).withMinute(0).withSecond(0)
            DateFilter.WEEK -> now.minusDays(7)
            DateFilter.MONTH -> now.withDayOfMonth(1).withHour(0).withMinute(0)
            DateFilter.YEAR -> now.withDayOfYear(1).withHour(0).withMinute(0)
        }
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        repository.searchTransactions(startMillis, endMillis, query).map { list ->
            when (sort) {
                SortOrder.DATE_DESC -> list.sortedByDescending { it.timestamp }
                SortOrder.DATE_ASC -> list.sortedBy { it.timestamp }
                SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
                SortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
            }
        }.onEach { _isLoading.value = false }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(300)
            _selectedFilter.value = _selectedFilter.value
            _isRefreshing.value = false
        }
    }

    fun syncSmsTransactions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                importSmsTransactionsUseCase.execute()
                _statusEvent.send("SMS sync complete")
            } catch (e: Exception) {
                _statusEvent.send("Sync failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when {
                transaction.smsId != null -> {
                    _statusEvent.send("SMS transactions cannot be deleted")
                }
                Duration.between(transaction.timestamp, LocalDateTime.now()).toMinutes() > 60 -> {
                    _statusEvent.send("Transactions older than 1 hour can only be edited")
                }
                else -> {
                    repository.deleteTransactionById(transaction.id)
                    _statusEvent.send("Transaction deleted")
                }
            }
        }
    }

    fun splitTransaction(parent: Transaction, splits: List<Pair<String, Double>>) {
        viewModelScope.launch {
            try {
                repository.deleteTransactionById(parent.id)
                val subTransactions = splits.map { (catName, amt) ->
                    parent.copy(
                        id = 0,
                        amount = amt,
                        categoryName = catName,
                        parentTransactionId = parent.id,
                        note = "Split from original ₹${parent.amount}: ${parent.note ?: ""}".trim()
                    )
                }
                repository.insertTransactions(subTransactions)
                _statusEvent.send("Transaction split successfully")
            } catch (e: Exception) {
                _statusEvent.send("Split failed: ${e.message}")
            }
        }
    }

    fun settleTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction.copy(isSettled = true))
                _statusEvent.send("Debt marked as settled")
            } catch (e: Exception) {
                _statusEvent.send("Settlement failed: ${e.message}")
            }
        }
    }
}
