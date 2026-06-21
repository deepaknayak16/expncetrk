package com.example.expncetracker.exptkr.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.domain.model.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    val summary = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts = accountDao.getAllAccounts()
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
            // Simulating refresh delay
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }

    fun addAccount(name: String, balance: Double, type: String) {
        viewModelScope.launch {
            val existing = accountDao.getAccountByName(name)
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
            accountDao.insertAccount(
                AccountEntity(
                    name = name,
                    balance = balance,
                    type = type,
                    color = color
                )
            )
            _statusEvent.send("Account created")
        }
    }

    fun updateAccount(id: Long, name: String, balance: Double, type: String) {
        viewModelScope.launch {
            val existing = accountDao.getAccountByName(name)
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
            accountDao.updateAccount(
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
            accountDao.deleteAccountAndTransactions(id)
            _statusEvent.send("Account deleted")
        }
    }
}
