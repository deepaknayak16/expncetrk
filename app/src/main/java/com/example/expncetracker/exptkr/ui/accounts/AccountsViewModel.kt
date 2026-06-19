package com.example.expncetracker.exptkr.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    val accounts = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    // FIX #16: Emit error when name already exists
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

    // FIX #16: Emit error when name collides with a different account
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
