package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun getAccountByName(name: String): AccountEntity?
    suspend fun insertAccount(account: AccountEntity)
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun deleteAccountAndTransactions(id: Long)
    suspend fun adjustBalance(name: String, delta: Double)
    suspend fun adjustBalanceById(id: Long, delta: Double)
}
