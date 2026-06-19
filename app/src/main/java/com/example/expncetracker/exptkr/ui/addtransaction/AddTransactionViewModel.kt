package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val goalRepository: GoalRepository // NEW for Step 3
) : ViewModel() {

    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit = _transactionToEdit.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

    fun loadTransactionForEdit(transaction: Transaction) {
        _transactionToEdit.value = transaction
    }

    fun clearEdit() {
        _transactionToEdit.value = null
    }

    fun addTransaction(
        id: Long = 0,
        amount: Double,
        accountId: Long,
        type: TransactionType,
        category: String,
        description: String?,
        merchant: String,
        bankName: String,
        timestamp: LocalDateTime = LocalDateTime.now(),
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _statusEvent.send("Amount must be greater than 0")
                return@launch
            }

            val transaction = Transaction(
                id = id,
                smsId = _transactionToEdit.value?.smsId,
                accountId = accountId,
                amount = amount,
                type = type,
                categoryName = category,
                description = description,
                merchant = merchant,
                bankName = bankName,
                timestamp = timestamp,
                isRecurring = isRecurring
            )

            if (id != 0L) {
                // Editing existing
                val old = _transactionToEdit.value
                repository.updateTransactionWithBalance(old, transaction)
                _statusEvent.send("Transaction updated")
                clearEdit()
            } else {
                // Adding new
                repository.insertTransactionWithBalance(transaction)
                _statusEvent.send("Transaction added")

                // FIX #17: Auto-recalculate goals linked to Savings/Investment
                if (category == "Savings" || category == "Investment") {
                    goalRepository.recalculateGoalsByCategory(category)
                }
            }
        }
    }
}
