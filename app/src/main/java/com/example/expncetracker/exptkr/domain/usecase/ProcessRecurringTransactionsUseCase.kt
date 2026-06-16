package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ProcessRecurringTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao
) {
    suspend fun execute() {
        val now = LocalDateTime.now()
        val currentMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val dueTransactions = repository.getDueRecurringTransactions(currentMillis)
        
        dueTransactions.forEach { tx ->
            // Create a new transaction instance for today
            val newTransaction = tx.copy(
                id = 0,
                smsId = null,
                timestamp = now,
                isRecurring = false, // The new instance is a normal transaction
                frequency = null,
                nextDueDate = null,
                recurrenceEndDate = null,
                parentTransactionId = tx.id
            )
            
            // 1. Insert the new transaction
            repository.insertTransaction(newTransaction)
            
            // 2. Update Account Balance
            val accounts = accountDao.getAllAccounts().first()
            val account = accounts.find { it.name == tx.bankName }
            account?.let { acc ->
                val newBalance = if (tx.type == TransactionType.CREDIT) {
                    acc.balance + tx.amount
                } else {
                    acc.balance - tx.amount
                }
                accountDao.updateAccount(acc.copy(balance = newBalance))
            }
            
            // 3. Update the next due date on the parent recurring transaction
            val nextDate = calculateNextDate(tx.nextDueDate ?: tx.timestamp, tx.frequency!!)
            val updatedParent = tx.copy(nextDueDate = nextDate)
            
            // Check if end date reached
            if (tx.recurrenceEndDate != null && nextDate.isAfter(tx.recurrenceEndDate)) {
                // Stop recurring
                repository.updateTransaction(updatedParent.copy(isRecurring = false))
            } else {
                repository.updateTransaction(updatedParent)
            }
        }
    }

    private fun calculateNextDate(current: LocalDateTime, frequency: RecurrenceFrequency): LocalDateTime {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> current.plusDays(1)
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }
}
