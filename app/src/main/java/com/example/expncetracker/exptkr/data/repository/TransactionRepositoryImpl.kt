package com.example.expncetracker.exptkr.data.repository

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
    private val accountDao: AccountDao
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

    override suspend fun deleteTransactionById(id: Long) {
        val transaction = transactionDao.getTransactionById(id)?.toDomain() ?: return
        transactionDao.deleteById(id)

        val delta = when (transaction.type) {
            TransactionType.CREDIT, TransactionType.BORROW -> -transaction.amount
            TransactionType.DEBIT, TransactionType.LEND -> transaction.amount
            else -> 0.0
        }
        if (delta != 0.0) {
            accountDao.adjustBalance(transaction.bankName, delta)
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
}
