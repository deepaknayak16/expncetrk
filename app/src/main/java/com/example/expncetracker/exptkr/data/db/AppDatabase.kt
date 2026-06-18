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
    version = 6,
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
                db.execSQL("ALTER TABLE transactions ADD COLUMN entryTimestamp INTEGER NOT NULL DEFAULT 0")
                // Update existing records to use their transaction timestamp as a fallback
                db.execSQL("UPDATE transactions SET entryTimestamp = timestamp WHERE entryTimestamp = 0")
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
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }
    }
}
