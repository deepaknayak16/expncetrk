package com.example.expncetracker.exptkr.data.db

// Import for SQLCipher database encryption factory
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Imports for your custom application utilities (Verify these match your packages)
import com.example.expncetracker.exptkr.core.common.SecurityUtils
import com.example.expncetracker.exptkr.core.common.Constants
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.BudgetDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.dao.RawSmsDao
import com.example.expncetracker.exptkr.data.db.dao.TransactionDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.data.db.entity.BudgetEntity
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.data.db.dao.GoalDao
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
// Ensure you have correct imports for these project files:
// import com.example.expncetracker.exptkr.utils.SecurityUtils
// import com.example.expncetracker.exptkr.utils.Constants
// import net.sqlcipher.database.SupportOpenHelperFactory


@Database(
    entities = [
        TransactionEntity::class,
        RawSmsEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        GoalEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun rawSmsDao(): RawSmsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao

    // Moved inside the class body
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                // Update existing records to use their transaction timestamp as a fallback
                db.execSQL("UPDATE transactions SET createdAt = timestamp WHERE createdAt = 0")
            }
        }
        // WHY: Migration 6→7 adds the account_id column so we can stop using name as a key.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER NOT NULL DEFAULT 0")
                // Optional: backfill account_id from bankName for existing rows
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
                // SQLite doesn't enforce unique constraints on existing duplicates easily.
                // First, deduplicate by renaming duplicates:
                db.execSQL("""
            DELETE FROM accounts WHERE rowid NOT IN (
                SELECT MIN(rowid) FROM accounts GROUP BY name
            )
        """)
                db.execSQL("CREATE UNIQUE INDEX index_accounts_name ON accounts(name)")
            }
        }
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            val databaseName = Constants.DATABASE_NAME

            // Load the native SQLCipher library safely
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                // Ignore if already loaded
            }

            val passphrase = SecurityUtils.getOrCreatePassphrase(appContext)
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(appContext, AppDatabase::class.java, databaseName)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
        }
    }
}
