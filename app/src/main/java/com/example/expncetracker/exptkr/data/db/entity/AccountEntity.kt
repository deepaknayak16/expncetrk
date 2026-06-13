package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double,
    val type: String, // e.g., "Cash", "Bank", "Credit Card"
    val color: Int // Store color as Int
)
