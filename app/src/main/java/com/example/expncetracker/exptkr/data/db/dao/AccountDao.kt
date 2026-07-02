package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

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

    @Query("SELECT id FROM accounts WHERE name LIKE :bankPrefix || '%' AND name LIKE '%' || :lastDigits LIMIT 1")
    suspend fun findAccountIdByDigits(bankPrefix: String, lastDigits: String): Long?

    @Transaction
    suspend fun adjustBalance(name: String, delta: java.math.BigDecimal): Int {
        val account = getAccountByName(name) ?: return 0
        updateBalance(account.id, account.balance.add(delta))
        return 1
    }

    @Transaction
    suspend fun adjustBalanceById(accountId: Long, delta: java.math.BigDecimal): Int {
        val account = getAccountById(accountId) ?: return 0
        updateBalance(account.id, account.balance.add(delta))
        return 1
    }

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("DELETE FROM transactions WHERE bankName = :name")
    suspend fun deleteTransactionsByBankName(name: String)

    @Query("DELETE FROM transactions WHERE account_id = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: Long)

    @Query("UPDATE goals SET linked_account_id = NULL WHERE linked_account_id = :accountId")
    suspend fun clearGoalLinksByAccountId(accountId: Long)

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("UPDATE accounts SET balance = :balance WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, balance: BigDecimal)

    @Query("UPDATE accounts SET balance = :balance WHERE name = :name")
    suspend fun updateBalanceByName(name: String, balance: BigDecimal)

    @Query("UPDATE accounts SET balance = 0.0")
    suspend fun resetAllBalances()

    // FIX: Manual cascade since ForeignKey was removed in Migration 11->12
    @Transaction
    suspend fun deleteAccountAndTransactions(accountId: Long) {
        clearGoalLinksByAccountId(accountId)
        deleteTransactionsByAccountId(accountId)
        deleteAccountById(accountId)
    }
}
