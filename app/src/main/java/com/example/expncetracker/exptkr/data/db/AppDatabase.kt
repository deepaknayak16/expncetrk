package com.example.expncetracker.exptkr.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expncetracker.exptkr.core.common.Constants
import com.example.expncetracker.exptkr.core.common.SecurityUtils // FIX #1
import com.example.expncetracker.exptkr.data.db.converter.Converters
import com.example.expncetracker.exptkr.data.db.dao.*
import com.example.expncetracker.exptkr.data.db.entity.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory // FIX #2

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        BudgetEntity::class // FIX #3: restored
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao // FIX #3: restored

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN frequency TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN nextDueDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceEndDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN parentTransactionId INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSettled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions RENAME COLUMN entryTimestamp TO createdAt")
                db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE transactions
                    SET account_id = (
                        SELECT id FROM accounts WHERE accounts.name = transactions.bankName LIMIT 1
                    )
                    WHERE account_id = 0
                """)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: For each duplicate account name, update transactions to point to the surviving account ID
                db.execSQL("""
                    UPDATE transactions
                        SET account_id = (
                            SELECT MIN(id) FROM accounts AS a2 WHERE a2.name = accounts.name
                            )
                        WHERE account_id IN (
                            SELECT id FROM accounts WHERE rowid NOT IN (
                                SELECT MIN(rowid) FROM accounts GROUP BY name
                                )
                            )
                        """)

                // Step 2: Delete duplicate accounts (transactions now point to the survivor)
                db.execSQL("""
                        DELETE FROM accounts WHERE rowid NOT IN (
                            SELECT MIN(rowid) FROM accounts GROUP BY name
                            )
                    """)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN linked_category TEXT")
                db.execSQL("ALTER TABLE goals ADD COLUMN linked_account_id INTEGER")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE transactions
                    SET account_id = (
                        SELECT id FROM accounts WHERE accounts.name = transactions.bankName LIMIT 1
                    )
                    WHERE account_id = 0
                    AND EXISTS (SELECT 1 FROM accounts WHERE accounts.name = transactions.bankName)
                """)
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            val databaseName = Constants.DATABASE_NAME

            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("SQLCipher native library failed to load", e)
            }

            val passphrase = SecurityUtils.getOrCreatePassphrase(appContext)
            val factory = SupportOpenHelperFactory(passphrase) // FIX #2

            return Room.databaseBuilder(appContext, AppDatabase::class.java, databaseName)
                .openHelperFactory(factory)
                .addMigrations(
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                .build()
        }
    }
}
