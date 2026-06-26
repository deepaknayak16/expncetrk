package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.AppDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ProcessRecurringTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val accountDao: AccountDao,
    private val db: AppDatabase
) {
    suspend fun execute() {
        val now = LocalDateTime.now()
        val currentMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val dueTransactions = repository.getDueRecurringTransactions(currentMillis)
        val allAccounts = accountDao.getAllAccounts().first()
        
        dueTransactions.forEach { tx ->
            db.withTransaction {
                val account = allAccounts.find { it.name == tx.bankName || it.id == tx.accountId }
                var totalDelta = java.math.BigDecimal.ZERO

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
                        createdAt = now,
                        idempotencyHash = null // Clear hash to avoid UNIQUE constraint collision with parent
                    )
                    repository.insertTransactionWithBalance(newTransaction)

                    val delta = when (newTransaction.type) {
                        TransactionType.CREDIT, TransactionType.BORROW -> newTransaction.amount
                        TransactionType.DEBIT, TransactionType.LEND -> newTransaction.amount.negate()
                        else -> java.math.BigDecimal.ZERO
                    }
                    totalDelta = totalDelta.add(delta)

                    nextDate = calculateNextDate(nextDate, tx.frequency!!)
                }

                // Persist the account balance once after catching up all periods.
                if (totalDelta != java.math.BigDecimal.ZERO) {
                    if (account != null) {
                        accountDao.adjustBalanceById(account.id, totalDelta)
                    } else {
                        com.example.expncetracker.exptkr.core.common.Logger.e("ProcessRecurring", "Balance drift: No account found for '${tx.bankName}' during recurring catch-up")
                    }
                }

                // Update the parent recurring transaction with the new next due date,
                // stopping recurrence if the end date has been reached.
                val ended = tx.recurrenceEndDate != null && nextDate.isAfter(tx.recurrenceEndDate)
                repository.updateTransaction(tx.copy(nextDueDate = nextDate, isRecurring = !ended))
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
