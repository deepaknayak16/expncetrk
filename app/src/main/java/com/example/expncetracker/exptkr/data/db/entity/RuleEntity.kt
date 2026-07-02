package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classification_rules",
    indices = [Index(value = ["keyword", "matchType", "transactionType"], unique = true)]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val category: String,
    val matchType: String = "CONTAINS", // EXACT or CONTAINS
    val transactionType: String? = null,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val isSystemRule: Boolean = false
)
