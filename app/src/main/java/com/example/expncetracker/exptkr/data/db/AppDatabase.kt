package com.example.expncetracker.exptkr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, RawSmsEntity::class, BudgetEntity::class, AccountEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun rawSmsDao(): RawSmsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
}
