package com.example.expncetracker.exptkr.data.repository

import androidx.room.withTransaction
import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.mapper.toDomain
import com.example.expncetracker.exptkr.data.mapper.toEntity
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val db: AppDatabase
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end).map { entities -> entities.map { it.toDomain() } }

    override fun searchTransactions(start: Long, end: Long, query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactionsInRange(start, end, query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction.toEntity())

    override suspend fun insertTransactions(transactions: List<Transaction>) =
        transactionDao.insertTransactions(transactions.map { it.toEntity() })

    // FIX #13: Fallback to bankName for legacy transactions with accountId == 0
    override suspend fun deleteTransactionById(id: Long) {
        val transaction = transactionDao.getTransactionById(id)?.toDomain() ?: return
        val delta = when (transaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> -transaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> transaction.amount
            else -> BigDecimal.ZERO
        }
        db.withTransaction {
            transactionDao.deleteById(id)
            if (delta.signum() != 0) {
                val rows = if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance adjustment failed for transaction $id: No matching account (ID=${transaction.accountId}, Name=${transaction.bankName})")
                }
            }
        }
    }

    override suspend fun getLatestTransactionTimestamp(): Long =
        transactionDao.getLatestTransactionTimestamp() ?: 0L

    override suspend fun getLatestSmsTimestamp(): Long =
        transactionDao.getLatestSmsTimestamp() ?: 0L

    override fun getAllRecurringTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllRecurringTransactions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDueRecurringTransactions(timestamp: Long): List<Transaction> =
        transactionDao.getDueRecurringTransactions(timestamp).map { it.toDomain() }

    override suspend fun clearAllTransactions() =
        transactionDao.clearAll()

    override suspend fun replaceTransactions(transactions: List<Transaction>) =
        transactionDao.replaceTransactions(transactions.map { it.toEntity() })

    override suspend fun splitTransaction(parentId: Long, subTransactions: List<Transaction>) =
        transactionDao.splitTransaction(parentId, subTransactions.map { it.toEntity() })

    // FIX #11: Use accountId when available; fallback to bankName for legacy
    override suspend fun insertTransactionWithBalance(transaction: Transaction): Long {
        val delta = when (transaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> transaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> -transaction.amount
            else -> BigDecimal.ZERO
        }
        return db.withTransaction {
            val id = transactionDao.insertTransaction(transaction.toEntity())
            if (id == -1L) return@withTransaction -1L // Duplicate SMS/transaction ignored

            if (delta.signum() != 0) {
                val rows = if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance adjustment failed for new transaction (hash=${transaction.idempotencyHash}): No matching account (ID=${transaction.accountId}, Name=${transaction.bankName})")
                }
            }
            id
        }
    }

    // FIX #11: Use accountId when available; fallback to bankName for legacy
    override suspend fun updateTransactionWithBalance(oldTransaction: Transaction?, newTransaction: Transaction) {
        val reverseDelta = oldTransaction?.let { old ->
            when (old.type) {
                TransactionType.CREDIT, TransactionType.BORROW -> -old.amount
                TransactionType.DEBIT, TransactionType.LEND -> old.amount
                else -> BigDecimal.ZERO
            }
        } ?: BigDecimal.ZERO

        val newDelta = when (newTransaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> newTransaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> -newTransaction.amount
            else -> BigDecimal.ZERO
        }

        db.withTransaction {
            if (oldTransaction != null && reverseDelta.signum() != 0) {
                val rows = if (oldTransaction.accountId != 0L) {
                    accountDao.adjustBalanceById(oldTransaction.accountId, reverseDelta)
                } else {
                    accountDao.adjustBalance(oldTransaction.bankName, reverseDelta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance reversal failed for update (ID=${oldTransaction.id}): No matching account")
                }
            }
            transactionDao.updateTransaction(newTransaction.toEntity())
            if (newDelta.signum() != 0) {
                val rows = if (newTransaction.accountId != 0L) {
                    accountDao.adjustBalanceById(newTransaction.accountId, newDelta)
                } else {
                    accountDao.adjustBalance(newTransaction.bankName, newDelta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance adjustment failed for update (ID=${newTransaction.id}): No matching account")
                }
            }
        }
    }

    // FIX #12: Use accountId when available; fallback to bankName for legacy
    override suspend fun settleTransaction(transaction: Transaction) {
        val delta = when (transaction.type) {
            TransactionType.LEND -> transaction.amount
            TransactionType.BORROW -> -transaction.amount
            else -> BigDecimal.ZERO
        }
        db.withTransaction {
            transactionDao.updateTransaction(transaction.copy(isSettled = true).toEntity())
            if (delta.signum() != 0) {
                val rows = if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance adjustment failed for settlement (ID=${transaction.id}): No matching account")
                }
            }
        }
    }

    // FIX #10: Atomic split — reverse parent + delete + insert children + apply children balances
    override suspend fun splitTransactionWithBalance(parent: Transaction, subTransactions: List<Transaction>) {
        val parentDelta = when (parent.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> -parent.amount
            TransactionType.DEBIT, TransactionType.LEND -> parent.amount
            else -> BigDecimal.ZERO
        }
        db.withTransaction {
            // Reverse parent balance
            if (parentDelta.signum() != 0) {
                val rows = if (parent.accountId != 0L) {
                    accountDao.adjustBalanceById(parent.accountId, parentDelta)
                } else {
                    accountDao.adjustBalance(parent.bankName, parentDelta)
                }
                if (rows == 0) {
                    Logger.e("TransactionRepo", "Balance reversal failed for split (Parent ID=${parent.id}): No matching account")
                }
            }
            // Delete parent and insert children
            transactionDao.deleteById(parent.id)
            val childrenEntities = subTransactions.map { it.toEntity() }
            transactionDao.insertTransactions(childrenEntities)
            // Apply children balances
            subTransactions.forEach { child ->
                val childDelta = when (child.type) {
                    TransactionType.CREDIT, TransactionType.BORROW -> child.amount
                    TransactionType.DEBIT, TransactionType.LEND -> -child.amount
                    else -> BigDecimal.ZERO
                }
                if (childDelta.signum() != 0) {
                    val rows = if (child.accountId != 0L) {
                        accountDao.adjustBalanceById(child.accountId, childDelta)
                    } else {
                        accountDao.adjustBalance(child.bankName, childDelta)
                    }
                    if (rows == 0) {
                        Logger.e("TransactionRepo", "Balance adjustment failed for split child: No matching account")
                    }
                }
            }
        }
    }

    override suspend fun sumAmountByCategory(category: String, type: String): BigDecimal =
        transactionDao.sumAmountByCategoryAndType(category, type)

    override suspend fun cleanupDuplicates() {
        transactionDao.deleteDuplicateSmsTransactions()
    }
}
