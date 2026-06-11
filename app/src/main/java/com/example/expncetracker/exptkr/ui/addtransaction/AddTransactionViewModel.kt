package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        description: String?
    ) {
        viewModelScope.launch {
            val categoryEnum = when (category.uppercase()) {
                "FOOD" -> Category.FOOD
                "CABS", "TRANSPORT" -> Category.CABS
                "RENT" -> Category.RENT
                "BILLS" -> Category.BILLS
                "SHOPPING" -> Category.SHOPPING
                "SALARY" -> Category.SALARY
                "INVESTMENT", "INVESTMENTS" -> Category.INVESTMENTS
                "TRAVEL" -> Category.TRAVEL
                else -> Category.OTHERS
            }
            
            val transaction = Transaction(
                id = 0,
                smsId = null, // Temporary ID for manual entries must not claim an smsId
                amount = amount,
                type = type,
                category = categoryEnum,
                merchant = description ?: "Manual Entry",
                bankName = "Manual",
                timestamp = java.time.LocalDateTime.now()
            )
            repository.insertTransactions(listOf(transaction))
        }
    }
}
