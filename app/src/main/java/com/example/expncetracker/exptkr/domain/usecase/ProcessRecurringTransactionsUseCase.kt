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
            val account = accountDao.getAllAccounts().first().find { it.name == tx.bankName }
            var accumulatedBalance = account?.balance

            // Generate one instance for every period that has elapsed since the last due date,
            // so missed periods (e.g. app/worker not run for a while) are caught up.
            var nextDate = tx.nextDueDate ?: tx.timestamp
            while (!nextDate.isAfter(now)) {
                // Stop generating once the recurrence end date is passed.
                if (tx.recurrenceEndDate != null && nextDate.isAfter(tx.recurrenceEndDate)) break

                val newTransaction = tx.copy(
                    id = 0,
                    smsId = null,
                    timestamp = nextDate,
                    isRecurring = false, // The new instance is a normal transaction
                    frequency = null,
                    nextDueDate = null,
                    recurrenceEndDate = null,
                    parentTransactionId = tx.id,
                    entryTimestamp = now
                )
                repository.insertTransaction(newTransaction)

                accumulatedBalance = accumulatedBalance?.let { balance ->
                    if (tx.type == TransactionType.CREDIT) balance + tx.amount else balance - tx.amount
                }

                nextDate = calculateNextDate(nextDate, tx.frequency!!)
            }

            // Persist the account balance once after catching up all periods.
            if (account != null && accumulatedBalance != null) {
                accountDao.updateAccount(account.copy(balance = accumulatedBalance))
            }

            // Update the parent recurring transaction with the new next due date,
            // stopping recurrence if the end date has been reached.
            val ended = tx.recurrenceEndDate != null && nextDate.isAfter(tx.recurrenceEndDate)
            repository.updateTransaction(tx.copy(nextDueDate = nextDate, isRecurring = !ended))
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
