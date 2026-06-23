package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    override suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)
    override suspend fun getAccountByName(name: String): AccountEntity? = accountDao.getAccountByName(name)
    override suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    override suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    override suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    override suspend fun deleteAccountAndTransactions(id: Long) = accountDao.deleteAccountAndTransactions(id)
    override suspend fun adjustBalance(name: String, delta: Double) {
        accountDao.adjustBalance(name, delta)
    }
    override suspend fun adjustBalanceById(id: Long, delta: Double) {
        accountDao.adjustBalanceById(id, delta)
    }
}
