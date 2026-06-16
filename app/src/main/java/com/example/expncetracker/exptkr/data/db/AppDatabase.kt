package com.example.expncetracker.exptkr.data.db

// Import for SQLCipher database encryption factory
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Imports for your custom application utilities (Verify these match your packages)
import com.example.expncetracker.exptkr.core.common.SecurityUtils
import com.example.expncetracker.exptkr.core.common.Constants
import android.content.Context // Added missing import
import androidx.room.Database
import androidx.room.Room // Added missing import
import androidx.room.RoomDatabase
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
    version = 5,
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            val databaseName = Constants.DATABASE_NAME

            fun createDb(): AppDatabase {
                val passphrase = SecurityUtils.getOrCreatePassphrase(appContext)
                val factory = SupportOpenHelperFactory(passphrase)

                return Room.databaseBuilder(appContext, AppDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .openHelperFactory(factory)
                    .build()
            }

            return try {
                val db = createDb()
                // Trigger a database open to verify the passphrase/integrity
                db.openHelper.writableDatabase
                db
            } catch (e: Exception) {
                // If decryption fails or file is corrupted, delete and recreate
                if (e.message?.contains("file is not a database", ignoreCase = true) == true ||
                    e is android.database.sqlite.SQLiteException) {
                    appContext.deleteDatabase(databaseName)
                    createDb()
                } else {
                    throw e
                }
            }
        }
    }
}
