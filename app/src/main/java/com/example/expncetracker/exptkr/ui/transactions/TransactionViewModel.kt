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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _statusEvent = Channel<String>()
    val statusEvent = _statusEvent.receiveAsFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(
        _selectedFilter.flatMapLatest { filter ->
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
            repository.getTransactionsInRange(startMillis, endMillis).onEach { _isLoading.value = false }
        },
        _searchQuery.debounce(300L)
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { 
            it.merchant.contains(query, ignoreCase = true) || 
            it.bankName.contains(query, ignoreCase = true) ||
            it.category.displayName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun refreshTransactions() {
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
}
