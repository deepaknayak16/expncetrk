package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE timestamp BETWEEN :start AND :end
        AND (merchant LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchTransactionsInRange(start: Long, end: Long, query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT MAX(timestamp) FROM transactions")
    suspend fun getLatestTransactionTimestamp(): Long?

    @Query("SELECT MAX(timestamp) FROM transactions WHERE smsId IS NOT NULL")
    suspend fun getLatestSmsTimestamp(): Long?

    @Query("SELECT * FROM transactions WHERE isRecurring = 1 ORDER BY nextDueDate ASC")
    fun getAllRecurringTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1 AND nextDueDate <= :timestamp")
    suspend fun getDueRecurringTransactions(timestamp: Long): List<TransactionEntity>

    // FIX #19: Renamed parameter from categoryName → category for consistency with DB column
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceTransactions(transactions: List<TransactionEntity>) {
        clearAll()
        insertTransactions(transactions)
    }

    @Transaction
    suspend fun splitTransaction(parentId: Long, children: List<TransactionEntity>) {
        deleteById(parentId)
        insertTransactions(children)
    }

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :category AND type = 'DEBIT'")
    suspend fun getTotalSpentByCategory(category: String): Double?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category = :category AND type = :type")
    suspend fun sumAmountByCategoryAndType(category: String, type: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE account_id = :accountId AND type = 'DEBIT'")
    suspend fun sumDebitsByAccount(accountId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE account_id = :accountId AND type = 'CREDIT'")
    suspend fun sumCreditsByAccount(accountId: Long): Double

    @Transaction
    suspend fun deleteDuplicateSmsTransactions() {
        // Find and delete transactions that have identical core properties but different IDs
        // This is a one-time cleanup to fix duplicates created by inconsistent hashing
        val duplicates = findDuplicateSmsTransactions()
        duplicates.forEach { id ->
            deleteById(id)
        }
    }

    @Query("""
        SELECT id FROM transactions 
        WHERE id NOT IN (
            SELECT MIN(id) FROM transactions 
            GROUP BY amount, timestamp, merchant, bankName
        )
        AND smsId IS NOT NULL
    """)
    suspend fun findDuplicateSmsTransactions(): List<Long>

    @Query("""
    SELECT
        COALESCE(SUM(CASE WHEN type = 'CREDIT' OR type = 'BORROW' THEN amount ELSE 0 END), 0.0) -
        COALESCE(SUM(CASE WHEN type = 'DEBIT' OR type = 'LEND' THEN amount ELSE 0 END), 0.0)
    FROM transactions
    WHERE account_id = :accountId
    """)
    suspend fun calculateNetBalanceByAccount(accountId: Long): Double
}
