package com.example.expncetracker.exptkr.di

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.AppDatabaseMigrations
import com.example.expncetracker.exptkr.data.db.DefaultClassificationRules
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import com.example.expncetracker.exptkr.data.db.dao.RuleDao
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
                    AppDatabaseMigrations.MIGRATION_17_18
                )
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedCategories(db)
                        seedRules(db)
                    }
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Seed categories if empty
                        val catCursor = db.query("SELECT COUNT(*) FROM categories")
                        if (catCursor.moveToFirst() && catCursor.getInt(0) == 0) {
                            seedCategories(db)
                        }
                        catCursor.close()

                        val cursor = db.query("SELECT COUNT(*) FROM classification_rules")
                        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                            seedRules(db)
                        }
                        cursor.close()
                    }
                })
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

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun database(): AppDatabase
    }

    private fun seedRules(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        DefaultClassificationRules.rules.forEach { rule ->
            val values = ContentValues().apply {
                put("pattern", rule.pattern)
                put("categoryName", rule.category)
                put("priority", rule.priority)
                put("isActive", 1)
                put("transactionType", rule.transactionType)
            }
            db.insert("classification_rules", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }

    private fun seedCategories(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        DefaultClassificationRules.categories.forEach { cat ->
            val values = ContentValues().apply {
                put("name", cat.name)
                put("type", cat.type)
                put("iconName", cat.iconName)
                put("color", cat.color.toInt())
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
        }
    }
}
