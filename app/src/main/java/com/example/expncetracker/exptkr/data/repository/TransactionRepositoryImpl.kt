package com.example.expncetracker.exptkr.data.repository

import androidx.room.withTransaction
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
            else -> 0.0
        }
        db.withTransaction {
            transactionDao.deleteById(id)
            if (delta != 0.0) {
                if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
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
            else -> 0.0
        }
        return db.withTransaction {
            val id = transactionDao.insertTransaction(transaction.toEntity())
            if (delta != 0.0) {
                if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
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
                else -> 0.0
            }
        } ?: 0.0

        val newDelta = when (newTransaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> newTransaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> -newTransaction.amount
            else -> 0.0
        }

        db.withTransaction {
            if (oldTransaction != null && reverseDelta != 0.0) {
                if (oldTransaction.accountId != 0L) {
                    accountDao.adjustBalanceById(oldTransaction.accountId, reverseDelta)
                } else {
                    accountDao.adjustBalance(oldTransaction.bankName, reverseDelta)
                }
            }
            transactionDao.updateTransaction(newTransaction.toEntity())
            if (newDelta != 0.0) {
                if (newTransaction.accountId != 0L) {
                    accountDao.adjustBalanceById(newTransaction.accountId, newDelta)
                } else {
                    accountDao.adjustBalance(newTransaction.bankName, newDelta)
                }
            }
        }
    }

    // FIX #12: Use accountId when available; fallback to bankName for legacy
    override suspend fun settleTransaction(transaction: Transaction) {
        val delta = when (transaction.type) {
            TransactionType.LEND -> transaction.amount
            TransactionType.BORROW -> -transaction.amount
            else -> 0.0
        }
        db.withTransaction {
            transactionDao.updateTransaction(transaction.copy(isSettled = true).toEntity())
            if (delta != 0.0) {
                if (transaction.accountId != 0L) {
                    accountDao.adjustBalanceById(transaction.accountId, delta)
                } else {
                    accountDao.adjustBalance(transaction.bankName, delta)
                }
            }
        }
    }

    // FIX #10: Atomic split — reverse parent + delete + insert children + apply children balances
    override suspend fun splitTransactionWithBalance(parent: Transaction, subTransactions: List<Transaction>) {
        val parentDelta = when (parent.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> -parent.amount
            TransactionType.DEBIT, TransactionType.LEND -> parent.amount
            else -> 0.0
        }
        db.withTransaction {
            // Reverse parent balance
            if (parentDelta != 0.0) {
                if (parent.accountId != 0L) {
                    accountDao.adjustBalanceById(parent.accountId, parentDelta)
                } else {
                    accountDao.adjustBalance(parent.bankName, parentDelta)
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
                    else -> 0.0
                }
                if (childDelta != 0.0) {
                    if (child.accountId != 0L) {
                        accountDao.adjustBalanceById(child.accountId, childDelta)
                    } else {
                        accountDao.adjustBalance(child.bankName, childDelta)
                    }
                }
            }
        }
    }

    override suspend fun sumAmountByCategory(category: String, type: String): Double =
        transactionDao.sumAmountByCategoryAndType(category, type)
}
