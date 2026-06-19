package com.example.expncetracker.exptkr.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val summary: StateFlow<FinancialSummary?> = combine(_refreshTrigger) { _ -> Unit }
        .flatMapLatest { getSummaryUseCase(DateFilter.MONTH) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountUiModel>> = accountDao.getAllAccounts()
        .map { entities ->
            entities.map { entity ->
                AccountUiModel(
                    id = entity.id,
                    name = entity.name,
                    balance = entity.balance,
                    type = entity.type,
                    color = entity.color
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    fun refreshAccounts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _refreshTrigger.value++
                delay(300)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun triggerAddAccount() {
        _showAddDialog.value = true
    }

    fun onDialogDismissed() {
        _showAddDialog.value = false
    }

    fun addAccount(name: String, balance: Double, type: String) {
        viewModelScope.launch {
            val existing = accountDao.getAccountByName(name)
            if (existing != null) {
                // Show error via a status channel or toast
                return@launch
            }
            val color = when (type) {
                "Bank Account" -> 0xFF3B82F6.toInt() // Blue
                "Cash" -> 0xFF10B981.toInt() // Green
                "Wallet" -> 0xFF8B5CF6.toInt() // Purple
                "Investment" -> 0xFF06B6D4.toInt() // Cyan
                "Credit Card" -> 0xFFEF4444.toInt() // Red
                else -> 0xFF64748B.toInt() // Slate
            }
            accountDao.insertAccount(
                AccountEntity(
                    name = name,
                    balance = balance,
                    type = type,
                    color = color
                )
            )
        }
    }

    fun updateAccount(id: Long, name: String, balance: Double, type: String) {
        viewModelScope.launch {
            // Check for name collision with a DIFFERENT account
            val existing = accountDao.getAccountByName(name)
            if (existing != null && existing.id != id) {
                // Name already taken by another account — show error
                return@launch
            }
            val color = when (type) {
                "Bank Account" -> 0xFF3B82F6.toInt() // Blue
                "Cash" -> 0xFF10B981.toInt() // Green
                "Wallet" -> 0xFF8B5CF6.toInt() // Purple
                "Investment" -> 0xFF06B6D4.toInt() // Cyan
                "Credit Card" -> 0xFFEF4444.toInt() // Red
                else -> 0xFF64748B.toInt() // Slate
            }
            accountDao.updateAccount(
                AccountEntity(
                    id = id,
                    name = name,
                    balance = balance,
                    type = type,
                    color = color
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            // WHY: Deleting an account must also delete its transactions,
            //      or the transaction history becomes inconsistent.
            //      We do this inside a Room @Transaction in the DAO so it's atomic.
            accountDao.deleteAccountAndTransactions(id)
        }
    }
}

data class AccountUiModel(
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val type: String,
    val color: Int
)
