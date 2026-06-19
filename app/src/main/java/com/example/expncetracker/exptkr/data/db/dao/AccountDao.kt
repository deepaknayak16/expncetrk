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

    @Query("UPDATE accounts SET balance = balance + :delta WHERE name = :name")
    suspend fun adjustBalance(name: String, delta: Double): Int

    // WHY: Look up and update balance by immutable ID instead of editable name.
    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun adjustBalanceById(accountId: Long, delta: Double): Int
    // WHY: Atomic delete — either both the account and its transactions disappear,
//      or neither does. No orphan transactions left behind.
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Transaction
    suspend fun deleteAccountAndTransactions(accountId: Long) {
        // If you have already done H4 and added account_id:
        // deleteTransactionsByAccountId(accountId)

        // If you have NOT done H4 yet (current code), you need to find the name first:
        val account = getAccountById(accountId) ?: return
        deleteTransactionsByBankName(account.name)
        deleteAccountById(accountId)
    }

    @Query("DELETE FROM transactions WHERE bankName = :name")
    suspend fun deleteTransactionsByBankName(name: String)

}
