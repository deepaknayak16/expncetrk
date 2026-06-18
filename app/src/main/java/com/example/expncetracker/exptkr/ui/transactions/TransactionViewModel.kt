package com.example.expncetracker.exptkr.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
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

data class TransactionFilter(
    val query: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val categoryName: String? = null,
    val accountName: String? = null
)

data class SmartFilter(
    val name: String,
    val filter: TransactionFilter
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase,
    private val accountDao: AccountDao,
    categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {
    
    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _advancedFilter = MutableStateFlow(TransactionFilter())
    val advancedFilter = _advancedFilter.asStateFlow()

    private val _smartFilters = MutableStateFlow<List<SmartFilter>>(emptyList())
    val smartFilters = _smartFilters.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = combine(
        _selectedFilter,
        _searchQuery
            .debounce(300L)
            .filter { it.length >= 2 || it.isEmpty() },
        _sortOrder,
        _advancedFilter,
        _refreshTrigger
    ) { filter, query, sort, adv, _ ->
        val now = LocalDateTime.now()
        val defaultStart = when (filter) {
            DateFilter.DAY -> now.withHour(0).withMinute(0).withSecond(0)
            DateFilter.WEEK -> now.minusDays(7)
            DateFilter.MONTH -> now.withDayOfMonth(1).withHour(0).withMinute(0)
            DateFilter.YEAR -> now.withDayOfYear(1).withHour(0).withMinute(0)
        }
        val startMillis = adv.startDate ?: defaultStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = adv.endDate ?: now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        FilterParams(startMillis, endMillis, query, sort, adv)
    }.flatMapLatest { params ->
        _isLoading.value = true
        repository.searchTransactions(params.startMillis, params.endMillis, params.query).map { list ->
            list.filter { tx ->
                val amountMatch = (params.adv.minAmount == null || tx.amount >= params.adv.minAmount) &&
                                 (params.adv.maxAmount == null || tx.amount <= params.adv.maxAmount)
                val categoryMatch = params.adv.categoryName == null || tx.categoryName == params.adv.categoryName
                val accountMatch = params.adv.accountName == null || tx.bankName == params.adv.accountName
                
                // Advanced text search across merchant, note, category, account
                val textMatch = if (params.adv.query.isNotEmpty()) {
                    tx.merchant.contains(params.adv.query, ignoreCase = true) ||
                    (tx.note?.contains(params.adv.query, ignoreCase = true) ?: false) ||
                    tx.categoryName.contains(params.adv.query, ignoreCase = true) ||
                    tx.bankName.contains(params.adv.query, ignoreCase = true)
                } else true

                amountMatch && categoryMatch && accountMatch && textMatch
            }.let { filteredList ->
                when (params.sort) {
                    SortOrder.DATE_DESC -> filteredList.sortedByDescending { it.timestamp }
                    SortOrder.DATE_ASC -> filteredList.sortedBy { it.timestamp }
                    SortOrder.AMOUNT_DESC -> filteredList.sortedByDescending { it.amount }
                    SortOrder.AMOUNT_ASC -> filteredList.sortedBy { it.amount }
                }
            }
        }.onEach { _isLoading.value = false }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterParams(
        val startMillis: Long,
        val endMillis: Long,
        val query: String,
        val sort: SortOrder,
        val adv: TransactionFilter
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun updateAdvancedFilter(update: (TransactionFilter) -> TransactionFilter) {
        _advancedFilter.update(update)
    }

    fun saveSmartFilter(name: String) {
        val currentFilter = _advancedFilter.value.copy(query = _searchQuery.value)
        _smartFilters.update { it + SmartFilter(name, currentFilter) }
    }

    fun applySmartFilter(smartFilter: SmartFilter) {
        _searchQuery.value = smartFilter.filter.query
        _advancedFilter.value = smartFilter.filter
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
            try {
                importSmsTransactionsUseCase.execute()
                _refreshTrigger.value++
            } catch (e: Exception) {
                // Ignore sync errors during background refresh
            } finally {
                _isRefreshing.value = false
            }
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
                Duration.between(transaction.entryTimestamp, LocalDateTime.now()).toMinutes() > 60 -> {
                    _statusEvent.send("Manual transactions older than 1 hour cannot be deleted")
                }
                else -> {
                    repository.deleteTransactionById(transaction.id)
                    // S4 FIX: Reverse account balance atomically
                    val delta = when (transaction.type) {
                        TransactionType.CREDIT, TransactionType.BORROW -> -transaction.amount
                        TransactionType.DEBIT, TransactionType.LEND -> transaction.amount
                        else -> 0.0
                    }
                    if (delta != 0.0) {
                        accountDao.adjustBalance(transaction.bankName, delta)
                    }
                    _statusEvent.send("Transaction deleted")
                }
            }
        }
    }

    fun splitTransaction(parent: Transaction, splits: List<Pair<String, Double>>) {
        viewModelScope.launch {
            try {
                reverseAccountBalance(parent)
                val subTransactions = splits.map { (catName, amt) ->
                    parent.copy(
                        id = 0,
                        amount = amt,
                        categoryName = catName,
                        parentTransactionId = parent.id,
                        note = "Split from original ₹${parent.amount}: ${parent.note ?: ""}".trim()
                    )
                }
                repository.splitTransaction(parent.id, subTransactions)
                _statusEvent.send("Transaction split successfully")
            } catch (e: Exception) {
                _statusEvent.send("Split failed: ${e.message}")
            }
        }
    }

    private suspend fun reverseAccountBalance(transaction: Transaction) {
        val delta = when (transaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> -transaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> transaction.amount
            else -> 0.0
        }
        if (delta != 0.0) {
            accountDao.adjustBalance(transaction.bankName, delta)
        }
    }

    fun settleTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction.copy(isSettled = true))
                // S4 FIX: Update account balance on settlement atomically
                // If LEND: money returned -> add to balance
                // If BORROW: money paid back -> subtract from balance
                val delta = when (transaction.type) {
                    TransactionType.LEND -> transaction.amount
                    TransactionType.BORROW -> -transaction.amount
                    else -> 0.0
                }
                if (delta != 0.0) {
                    accountDao.adjustBalance(transaction.bankName, delta)
                }
                _statusEvent.send("Debt marked as settled")
            } catch (e: Exception) {
                _statusEvent.send("Settlement failed: ${e.message}")
            }
        }
    }
}
