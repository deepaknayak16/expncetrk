package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classification_rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val categoryName: String,
    val transactionType: String? = null, // e.g., "CREDIT" or "DEBIT". Null means applies to both.
    val priority: Int = 0,
    val isActive: Boolean = true
)
