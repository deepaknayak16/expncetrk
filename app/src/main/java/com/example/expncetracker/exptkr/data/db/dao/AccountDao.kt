package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT * FROM accounts WHERE name = :name")
    suspend fun getAccountByName(name: String): AccountEntity?

    @Query("SELECT id FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountIdByName(name: String): Long?

    @Query("UPDATE accounts SET balance = balance + :delta WHERE name = :name")
    suspend fun adjustBalance(name: String, delta: Double): Int

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun adjustBalanceById(accountId: Long, delta: Double): Int

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("DELETE FROM transactions WHERE bankName = :name")
    suspend fun deleteTransactionsByBankName(name: String)

    @Query("DELETE FROM transactions WHERE account_id = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: Long)

    // FIX: Manual cascade since ForeignKey was removed in Migration 11->12
    @Transaction
    suspend fun deleteAccountAndTransactions(accountId: Long) {
        deleteTransactionsByAccountId(accountId)
        deleteAccountById(accountId)
    }
}
