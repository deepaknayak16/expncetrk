package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsId"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["account_id"]),
        Index(value = ["idempotencyHash"], unique = true),
        Index(value = ["parentTransactionId", "timestamp"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category", "type"]),
        Index(value = ["isRecurring", "nextDueDate"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsId: String?,
    val amount: java.math.BigDecimal,
    val type: String,
    val category: String,
    val merchant: String,
    val bankName: String,
    val note: String?,
    val timestamp: Long,
    val isRecurring: Boolean = false,
    val frequency: String? = null,
    val nextDueDate: Long? = null,
    val recurrenceEndDate: Long? = null,
    val parentTransactionId: Long? = null,
    val counterparty: String? = null,
    val isSettled: Boolean = false,
    val tags: String? = null,
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "account_id", defaultValue = "0")
    val accountId: Long = 0,

    // Phase 1 Additions
    val idempotencyHash: String? = null,
    val confidenceScore: Float = 1.0f,
    val parsingStatus: String = "COMPLETE",
    val isCategoryManuallyCorrected: Boolean = false
)
