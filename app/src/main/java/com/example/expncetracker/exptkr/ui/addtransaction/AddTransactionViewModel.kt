package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.domain.model.Category
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

    fun addTransaction(
        id: Long = 0,
        amount: Double,
        type: TransactionType,
        category: String,
        description: String?,
        bankName: String = "Manual",
        timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ) {
        viewModelScope.launch {
            if (_transactionToEdit.value?.smsId != null) {
                // Should not happen as UI prevents editing SMS transactions
                return@launch
            }

            val transaction = Transaction(
                id = id,
                smsId = _transactionToEdit.value?.smsId, // Keep smsId if editing
                amount = amount,
                type = type,
                categoryName = category,
                merchant = description ?: "Manual Entry",
                bankName = bankName,
                timestamp = timestamp
            )
            repository.insertTransactions(listOf(transaction))
        }
    }
}
