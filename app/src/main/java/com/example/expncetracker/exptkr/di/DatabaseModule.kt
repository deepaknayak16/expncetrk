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
import com.example.expncetracker.exptkr.data.db.dao.RuleDao
import com.example.expncetracker.exptkr.data.db.dao.SmsQuarantineDao
import com.example.expncetracker.exptkr.data.db.dao.RecurringTemplateDao
import com.example.expncetracker.exptkr.data.db.dao.MerchantStatsDao
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
                    AppDatabaseMigrations.MIGRATION_12_13,
                    AppDatabaseMigrations.MIGRATION_13_14,
                    AppDatabaseMigrations.MIGRATION_14_15,
                    AppDatabaseMigrations.MIGRATION_15_16,
                    AppDatabaseMigrations.MIGRATION_16_17,
                    AppDatabaseMigrations.MIGRATION_17_18,
                    AppDatabaseMigrations.MIGRATION_18_19,
                    AppDatabaseMigrations.MIGRATION_19_20,
                    AppDatabaseMigrations.MIGRATION_20_21,
                    AppDatabaseMigrations.MIGRATION_21_22,
                    AppDatabaseMigrations.MIGRATION_22_23,
                    AppDatabaseMigrations.MIGRATION_23_24,
                    AppDatabaseMigrations.MIGRATION_24_25,
                    AppDatabaseMigrations.MIGRATION_25_26
                )
                .fallbackToDestructiveMigration()
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
    @Provides fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()
    @Provides fun provideSmsQuarantineDao(db: AppDatabase): SmsQuarantineDao = db.smsQuarantineDao()
    @Provides fun provideRecurringTemplateDao(db: AppDatabase): RecurringTemplateDao = db.recurringTemplateDao()
    @Provides fun provideMerchantStatsDao(db: AppDatabase): MerchantStatsDao = db.merchantStatsDao()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun database(): AppDatabase
    }
}
