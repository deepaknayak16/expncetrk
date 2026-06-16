package com.example.expncetracker.exptkr.di

import android.content.Context
import androidx.room.Room
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.expncetracker.exptkr.core.common.SecurityUtils

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Load the native SQLCipher library
        System.loadLibrary("sqlcipher")

        // Create/Retrieve passphrase for encrypted database from Android Keystore
        val passphrase = SecurityUtils.getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .openHelperFactory(factory) // Enable SQLCipher encryption
            .build()
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideRawSmsDao(db: AppDatabase): RawSmsDao = db.rawSmsDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideGoalDao(db: AppDatabase): com.example.expncetracker.exptkr.data.db.dao.GoalDao = db.goalDao()
}
