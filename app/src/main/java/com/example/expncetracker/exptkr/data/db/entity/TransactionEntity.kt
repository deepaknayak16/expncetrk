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
        Index(value = ["account_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE // FIXED: was SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsId: Long?,
    val amount: Double,
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
)
