package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo


@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsId"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["account_id"])  // Add index
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL  // or CASCADE
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

    // WHY: Link transactions to accounts by immutable ID, not by name.Names can be edited; IDs cannot.

    @ColumnInfo(name = "account_id", defaultValue = "0")
    val accountId: Long = 0,
)
