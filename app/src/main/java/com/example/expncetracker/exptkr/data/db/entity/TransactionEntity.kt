package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["smsId"], unique = true)]
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
    val entryTimestamp: Long = System.currentTimeMillis()
)
