package com.example.expncetracker.exptkr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expncetracker.exptkr.data.db.dao.*
import com.example.expncetracker.exptkr.data.db.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        BudgetEntity::class,
        RawSmsEntity::class,
        MerchantMappingEntity::class,
        RuleEntity::class,
        SmsQuarantineEntity::class,
        RecurringTemplateEntity::class,
        MerchantStatsEntity::class
    ],
    version = 23,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun rawSmsDao(): RawSmsDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun ruleDao(): RuleDao
    abstract fun smsQuarantineDao(): SmsQuarantineDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    abstract fun merchantStatsDao(): MerchantStatsDao
}

object AppDatabaseMigrations {
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `merchant_stats` (
                    `merchantName` TEXT NOT NULL, 
                    `amountMean` REAL NOT NULL, 
                    `amountM2` REAL NOT NULL, 
                    `amountCount` INTEGER NOT NULL, 
                    `lastSeenTimestamp` INTEGER NOT NULL, 
                    `intervalMeanDays` REAL NOT NULL, 
                    `intervalM2` REAL NOT NULL, 
                    `intervalCount` INTEGER NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `updatedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`merchantName`)
                )
            """)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN isLiquid INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN rawSmsBody TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN smsFingerprint TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurringState TEXT NOT NULL DEFAULT 'NONE'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN cleanMerchantName TEXT")
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `sms_quarantine` (
                    `smsId` TEXT NOT NULL, 
                    `body` TEXT NOT NULL, 
                    `address` TEXT NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `errorReason` TEXT, 
                    `quarantineDate` INTEGER NOT NULL, 
                    PRIMARY KEY(`smsId`)
                )
            """)
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `recurring_templates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `merchantName` TEXT NOT NULL, 
                    `cleanMerchantName` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `frequency` TEXT NOT NULL, 
                    `nextDueDate` INTEGER NOT NULL, 
                    `state` TEXT NOT NULL DEFAULT 'ACTIVE', 
                    `lastDetectedDate` INTEGER NOT NULL, 
                    `confidenceScore` REAL NOT NULL
                )
            """)
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
            db.execSQL("""
                UPDATE transactions
                    SET account_id = (
                        SELECT MIN(id) FROM accounts WHERE name = transactions.bankName
                    )
                    WHERE account_id IN (
                        SELECT id FROM accounts WHERE rowid NOT IN (
                            SELECT MIN(rowid) FROM accounts GROUP BY name
                        )
                    )
                    """)
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

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN idempotencyHash TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN confidenceScore REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN parsingStatus TEXT NOT NULL DEFAULT 'COMPLETE'")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_idempotencyHash` ON `transactions` (`idempotencyHash`)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `raw_sms` (
                    `smsId` TEXT NOT NULL, 
                    `body` TEXT NOT NULL, 
                    `address` TEXT NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `parsingStatus` TEXT NOT NULL DEFAULT 'PENDING', 
                    PRIMARY KEY(`smsId`)
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `merchant_mappings` (
                    `merchantName` TEXT NOT NULL, 
                    `categoryName` TEXT NOT NULL, 
                    `updatedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`merchantName`)
                )
            """)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // To remove a Foreign Key in SQLite, we must recreate the table
            // 1. Create a temporary table with the new schema (no FK)
            db.execSQL("""
                CREATE TABLE transactions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    smsId TEXT,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    merchant TEXT NOT NULL,
                    bankName TEXT NOT NULL,
                    note TEXT,
                    timestamp INTEGER NOT NULL,
                    isRecurring INTEGER NOT NULL,
                    frequency TEXT,
                    nextDueDate INTEGER,
                    recurrenceEndDate INTEGER,
                    parentTransactionId INTEGER,
                    counterparty TEXT,
                    isSettled INTEGER NOT NULL,
                    tags TEXT,
                    createdAt INTEGER NOT NULL,
                    account_id INTEGER NOT NULL DEFAULT 0,
                    idempotencyHash TEXT,
                    confidenceScore REAL NOT NULL,
                    parsingStatus TEXT NOT NULL
                )
            """)

            // 2. Copy data from old table to new table
            db.execSQL("""
                INSERT INTO transactions_new (
                    id, smsId, amount, type, category, merchant, bankName, note, timestamp,
                    isRecurring, frequency, nextDueDate, recurrenceEndDate, parentTransactionId,
                    counterparty, isSettled, tags, createdAt, account_id, idempotencyHash,
                    confidenceScore, parsingStatus
                )
                SELECT 
                    id, smsId, amount, type, category, merchant, bankName, note, timestamp,
                    isRecurring, frequency, nextDueDate, recurrenceEndDate, parentTransactionId,
                    counterparty, isSettled, tags, createdAt, account_id, idempotencyHash,
                    confidenceScore, parsingStatus
                FROM transactions
            """)

            // 3. Drop old table
            db.execSQL("DROP TABLE transactions")

            // 4. Rename new table to original name
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

            // 5. Re-create indices
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_smsId` ON `transactions` (`smsId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_idempotencyHash` ON `transactions` (`idempotencyHash`)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budgets ADD COLUMN lastAlertSentAt INTEGER")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_parentTransactionId_timestamp` ON `transactions` (`parentTransactionId`, `timestamp`)")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Placeholder for missing version 15
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN isCategoryManuallyCorrected INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `classification_rules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `pattern` TEXT NOT NULL, 
                    `categoryName` TEXT NOT NULL, 
                    `priority` INTEGER NOT NULL, 
                    `isActive` INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE classification_rules ADD COLUMN transactionType TEXT")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
            CREATE TABLE raw_sms_new (
                smsId TEXT NOT NULL,
                body TEXT NOT NULL,
                address TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                parsingStatus TEXT NOT NULL DEFAULT 'PENDING',
                PRIMARY KEY(smsId)
            )
        """)
            db.execSQL("""
            INSERT OR IGNORE INTO raw_sms_new (smsId, body, address, timestamp, parsingStatus)
            SELECT CAST(smsId AS TEXT), body, address, timestamp, parsingStatus FROM raw_sms
        """)
            db.execSQL("DROP TABLE raw_sms")
            db.execSQL("ALTER TABLE raw_sms_new RENAME TO raw_sms")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `classification_rules_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `keyword` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `matchType` TEXT NOT NULL DEFAULT 'CONTAINS', 
                    `transactionType` TEXT, 
                    `priority` INTEGER NOT NULL DEFAULT 0, 
                    `isActive` INTEGER NOT NULL DEFAULT 1
                )
            """)
            db.execSQL("""
                INSERT INTO classification_rules_new (id, keyword, category, transactionType, priority, isActive)
                SELECT id, pattern, categoryName, transactionType, priority, isActive FROM classification_rules
            """)
            db.execSQL("DROP TABLE classification_rules")
            db.execSQL("ALTER TABLE classification_rules_new RENAME TO classification_rules")
        }
    }
}
