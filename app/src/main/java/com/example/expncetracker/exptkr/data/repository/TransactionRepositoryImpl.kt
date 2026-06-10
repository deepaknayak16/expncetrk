package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.mapper.toDomain
import com.example.expncetracker.exptkr.data.mapper.toEntity
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    override suspend fun insertTransactions(transactions: List<Transaction>) =
        transactionDao.insertTransactions(transactions.map { it.toEntity() })

    override suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteById(id)

    override suspend fun getLatestTransactionTimestamp(): Long =
        transactionDao.getLatestTransactionTimestamp() ?: 0L

    override suspend fun getLatestSmsTimestamp(): Long =
        transactionDao.getLatestSmsTimestamp() ?: 0L

    override suspend fun clearAllTransactions() =
        transactionDao.clearAll()
}
