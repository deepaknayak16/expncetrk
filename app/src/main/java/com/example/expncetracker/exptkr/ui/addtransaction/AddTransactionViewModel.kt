package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao,
    private val categoryDetector: com.example.expncetracker.exptkr.core.parser.CategoryDetector,
    private val categoryDao: com.example.expncetracker.exptkr.data.db.dao.CategoryDao
) : ViewModel() {

    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit = _transactionToEdit.asStateFlow()

    val categories: StateFlow<List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountUiModel>> = accountDao.getAllAccounts()
        .map { entities ->
            entities.map { entity ->
                AccountUiModel(
                    id = entity.id,
                    name = entity.name,
                    balance = entity.balance,
                    type = entity.type,
                    icon = androidx.compose.material.icons.Icons.Default.AccountBalanceWallet,
                    color = androidx.compose.ui.graphics.Color(entity.color)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(id)
            _transactionToEdit.value = tx
        }
    }

    fun onMerchantNameChanged(name: String, currentCategory: String) {
        viewModelScope.launch {
            val suggestedCategory = categoryDetector.detect(name, TransactionType.DEBIT)
            if (suggestedCategory != Category.OTHERS.displayName && (currentCategory.isEmpty() || currentCategory == Category.OTHERS.displayName)) {
                _suggestedCategory.value = suggestedCategory
            }
        }
    }

    private val _suggestedCategory = MutableStateFlow<String?>(null)
    val suggestedCategory = _suggestedCategory.asStateFlow()

    fun addTransaction(
        id: Long = 0,
        amount: Double,
        type: TransactionType,
        category: String,
        description: String?,
        note: String? = null,
        bankName: String = "Manual",
        counterparty: String? = null,
        isRecurring: Boolean = false,
        frequency: RecurrenceFrequency? = null,
        recurrenceEndDate: java.time.LocalDateTime? = null,
        tags: List<String> = emptyList(),
        timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ) {
        viewModelScope.launch {
            if (_transactionToEdit.value?.smsId != null) {
                return@launch
            }

            // Update Account Balance
            val accountEntities = accountDao.getAllAccounts().first()
            val account = accountEntities.find { it.name == bankName }
            
            account?.let { acc ->
                var newBalance = acc.balance
                
                // If editing, first reverse the old transaction
                _transactionToEdit.value?.let { old ->
                    if (old.bankName == bankName) {
                        newBalance = when (old.type) {
                            TransactionType.CREDIT, TransactionType.BORROW -> newBalance - old.amount
                            TransactionType.DEBIT, TransactionType.LEND -> newBalance + old.amount
                            TransactionType.TRANSFER -> newBalance
                        }
                    }
                }
                
                // Apply the new transaction
                newBalance = when (type) {
                    TransactionType.CREDIT, TransactionType.BORROW -> newBalance + amount
                    TransactionType.DEBIT, TransactionType.LEND -> newBalance - amount
                    TransactionType.TRANSFER -> newBalance
                }
                
                accountDao.updateAccount(acc.copy(balance = newBalance))
            }

            // Calculate next due date if it is recurring
            val nextDueDate = if (isRecurring && frequency != null) {
                calculateNextDate(timestamp, frequency)
            } else null

            val transaction = Transaction(
                id = id,
                smsId = _transactionToEdit.value?.smsId,
                amount = amount,
                type = type,
                categoryName = category,
                merchant = description ?: "Manual Entry",
                bankName = bankName,
                note = note,
                timestamp = timestamp,
                isRecurring = isRecurring,
                frequency = frequency,
                nextDueDate = nextDueDate,
                recurrenceEndDate = recurrenceEndDate,
                counterparty = counterparty,
                tags = tags
            )
            repository.insertTransactions(listOf(transaction))
        }
    }

    private fun calculateNextDate(current: java.time.LocalDateTime, frequency: RecurrenceFrequency): java.time.LocalDateTime {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> current.plusDays(1)
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }
}
