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
    val timestamp: Long
)
