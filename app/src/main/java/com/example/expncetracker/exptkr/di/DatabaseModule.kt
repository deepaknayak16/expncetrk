package com.example.expncetracker.exptkr.di

import android.content.Context
import android.database.sqlite.SQLiteException
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
package com.example.expncetracker.exptkr.di

import android.content.Context
import androidx.room.Room
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
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
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            // Ignore if already loaded
        }

        val passphrase = SecurityUtils.getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)
        try {
            return Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
                .openHelperFactory(factory)
                .addMigrations(
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8,
                    AppDatabase.MIGRATION_8_9,   // FIX #6
                    AppDatabase.MIGRATION_9_10   // FIX #6
                )
                .build()
        } catch (e: RuntimeException) { // FIX #13: catch broader type
            throw IllegalStateException("Database corrupted. Restore from backup or clear app data.", e)
        }
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    // FIX #3: removed provideRawSmsDao (RawSmsDao.kt doesn't exist)
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideGoalDao(db: AppDatabase): com.example.expncetracker.exptkr.data.db.dao.GoalDao = db.goalDao()
}
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Load the native SQLCipher library safely
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            // Ignore if already loaded or handle appropriately
        }

        // Create/Retrieve passphrase for encrypted database from Android Keystore
        val passphrase = SecurityUtils.getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)
        try {
            return Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
                .openHelperFactory(factory) // Enable SQLCipher encryption
                .addMigrations(
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8
                ) // Add known migrations
                .build()
        } catch (e: SQLiteException) {
            throw IllegalStateException("Database corrupted. Restore from backup or clear app data.", e)
        }
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideRawSmsDao(db: AppDatabase): RawSmsDao = db.rawSmsDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideGoalDao(db: AppDatabase): com.example.expncetracker.exptkr.data.db.dao.GoalDao = db.goalDao()
}
