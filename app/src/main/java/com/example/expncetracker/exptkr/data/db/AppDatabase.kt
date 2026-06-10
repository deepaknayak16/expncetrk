package com.example.expncetracker.exptkr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, RawSmsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun rawSmsDao(): RawSmsDao
}
