package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>>
    fun searchTransactions(start: Long, end: Long, query: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun deleteTransactionById(id: Long)
    suspend fun getLatestTransactionTimestamp(): Long
    suspend fun getLatestSmsTimestamp(): Long
    suspend fun clearAllTransactions()
}
