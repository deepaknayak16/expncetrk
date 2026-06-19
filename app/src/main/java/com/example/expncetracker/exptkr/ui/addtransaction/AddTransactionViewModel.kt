package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val accounts = accountDao.getAllAccounts()
        .map { list -> list.map { AccountUiModel(it.id, it.name, it.balance, it.type, it.color) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestedCategory = MutableStateFlow<String?>(null)
    val suggestedCategory = _suggestedCategory.asStateFlow()

    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit = _transactionToEdit.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            _transactionToEdit.value = repository.getTransactionById(transactionId)
        }
    }

    fun loadTransactionForEdit(transaction: Transaction) {
        _transactionToEdit.value = transaction
    }

    fun clearEdit() {
        _transactionToEdit.value = null
    }

    fun onMerchantNameChanged(merchant: String, currentCategory: String) {
        viewModelScope.launch {
            val suggestion = when {
                merchant.contains("grocery", ignoreCase = true) -> "Food"
                merchant.contains("restaurant", ignoreCase = true) -> "Food"
                merchant.contains("uber", ignoreCase = true) -> "Transport"
                merchant.contains("amazon", ignoreCase = true) -> "Shopping"
                merchant.contains("fuel", ignoreCase = true) -> "Transport"
                merchant.contains("rent", ignoreCase = true) -> "Housing"
                else -> null
            }
            if (suggestion != null && (currentCategory.isEmpty() || currentCategory == "Others")) {
                _suggestedCategory.value = suggestion
            }
        }
    }

    // FIX: Added accountId parameter (with default 0L for backward compat)
    fun addTransaction(
        id: Long = 0,
        amount: Double,
        type: TransactionType,
        category: String,
        merchant: String,
        note: String? = null,
        bankName: String,
        accountId: Long = 0L, // <-- ADDED
        counterparty: String? = null,
        isRecurring: Boolean = false,
        frequency: RecurrenceFrequency? = null,
        tags: List<String> = emptyList(),
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _statusEvent.send("Amount must be greater than 0")
                return@launch
            }

            // Use passed accountId if valid; otherwise fall back to bankName lookup
            val resolvedAccountId = if (accountId != 0L) accountId else (accountDao.getAccountIdByName(bankName) ?: 0L)

            val transaction = Transaction(
                id = id,
                smsId = _transactionToEdit.value?.smsId,
                accountId = resolvedAccountId,
                amount = amount,
                type = type,
                categoryName = category,
                merchant = merchant,
                bankName = bankName,
                note = note,
                timestamp = timestamp,
                isRecurring = isRecurring,
                frequency = frequency,
                counterparty = counterparty,
                tags = tags
            )

            if (id != 0L) {
                val old = _transactionToEdit.value
                repository.updateTransactionWithBalance(old, transaction)
                _statusEvent.send("Transaction updated")
                if (category == "Savings" || category == "Investment") {
                    goalRepository.recalculateGoalsByCategory(category)
                }
                clearEdit()
            } else {
                repository.insertTransactionWithBalance(transaction)
                _statusEvent.send("Transaction added")
                if (category == "Savings" || category == "Investment") {
                    goalRepository.recalculateGoalsByCategory(category)
                }
            }
        }
    }
}
