package com.example.expncetracker.exptkr.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.repository.AccountRepository
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getSummaryUseCase: GetSummaryUseCase,
    private val importSmsTransactionsUseCase: com.example.expncetracker.exptkr.domain.usecase.ImportSmsTransactionsUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    val summary = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts = accountRepository.getAllAccounts()
        .map { list ->
            list.map { entity ->
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

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    fun triggerAddAccount() {
        _showAddDialog.value = true
    }

    fun onDialogDismissed() {
        _showAddDialog.value = false
    }

    fun refreshAccounts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                importSmsTransactionsUseCase.execute()
                _statusEvent.send("Accounts refreshed from SMS")
            } catch (e: Exception) {
                _statusEvent.send("Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addAccount(name: String, balance: BigDecimal, type: String) {
        viewModelScope.launch {
            val existing = accountRepository.getAccountByName(name)
            if (existing != null) {
                _statusEvent.send("An account named \"$name\" already exists")
                return@launch
            }
            val color = when (type) {
                "Bank Account" -> 0xFF3B82F6.toInt()
                "Cash" -> 0xFF10B981.toInt()
                "Wallet" -> 0xFFF59E0B.toInt()
                "Credit Card" -> 0xFFEF4444.toInt()
                else -> 0xFF64748B.toInt()
            }
            try {
                accountRepository.insertAccount(
                    AccountEntity(
                        name = name,
                        balance = balance,
                        type = type,
                        color = color
                    )
                )
                _statusEvent.send("Account created")
            } catch (e: Exception) {
                if (e is android.database.sqlite.SQLiteConstraintException) {
                    _statusEvent.send("An account named \"$name\" already exists")
                } else {
                    _statusEvent.send("Failed to create account: ${e.message}")
                }
            }
        }
    }

    fun updateAccount(id: Long, name: String, balance: BigDecimal, type: String) {
        viewModelScope.launch {
            val existing = accountRepository.getAccountByName(name)
            if (existing != null && existing.id != id) {
                _statusEvent.send("An account named \"$name\" already exists")
                return@launch
            }
            val color = when (type) {
                "Bank Account" -> 0xFF3B82F6.toInt()
                "Cash" -> 0xFF10B981.toInt()
                "Wallet" -> 0xFFF59E0B.toInt()
                "Credit Card" -> 0xFFEF4444.toInt()
                else -> 0xFF64748B.toInt()
            }
            accountRepository.updateAccount(
                AccountEntity(
                    id = id,
                    name = name,
                    balance = balance,
                    type = type,
                    color = color
                )
            )
            _statusEvent.send("Account updated")
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccountAndTransactions(id)
            _statusEvent.send("Account deleted")
        }
    }
}
