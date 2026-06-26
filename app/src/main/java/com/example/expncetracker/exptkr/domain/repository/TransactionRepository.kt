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
    fun getAllRecurringTransactions(): Flow<List<Transaction>>
    suspend fun getDueRecurringTransactions(timestamp: Long): List<Transaction>
    suspend fun clearAllTransactions()

    suspend fun replaceTransactions(transactions: List<Transaction>)

    suspend fun splitTransaction(parentId: Long, subTransactions: List<Transaction>)

    suspend fun insertTransactionWithBalance(transaction: Transaction): Long
    suspend fun updateTransactionWithBalance(oldTransaction: Transaction?, newTransaction: Transaction)

    suspend fun settleTransaction(transaction: Transaction)

    // NEW for Step 2
    suspend fun splitTransactionWithBalance(parent: Transaction, subTransactions: List<Transaction>)
    suspend fun sumAmountByCategory(category: String, type: String): java.math.BigDecimal
    
    suspend fun cleanupDuplicates()
}
