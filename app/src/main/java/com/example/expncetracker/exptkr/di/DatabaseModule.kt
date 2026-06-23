package com.example.expncetracker.exptkr.di

import android.content.Context
import androidx.room.Room
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.AppDatabaseMigrations
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
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
                    AppDatabaseMigrations.MIGRATION_4_5,
                    AppDatabaseMigrations.MIGRATION_5_6,
                    AppDatabaseMigrations.MIGRATION_6_7,
                    AppDatabaseMigrations.MIGRATION_7_8,
                    AppDatabaseMigrations.MIGRATION_8_9,
                    AppDatabaseMigrations.MIGRATION_9_10,
                    AppDatabaseMigrations.MIGRATION_10_11,
                    AppDatabaseMigrations.MIGRATION_11_12,
                    AppDatabaseMigrations.MIGRATION_12_13
                )
                .build()
        } catch (e: RuntimeException) {
            throw IllegalStateException("Database corrupted. Restore from backup or clear app data.", e)
        }
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideGoalDao(db: AppDatabase): com.example.expncetracker.exptkr.data.db.dao.GoalDao = db.goalDao()
    @Provides fun provideMerchantMappingDao(db: AppDatabase): MerchantMappingDao = db.merchantMappingDao()
    @Provides fun provideRawSmsDao(db: AppDatabase): RawSmsDao = db.rawSmsDao()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun database(): AppDatabase
    }
}
