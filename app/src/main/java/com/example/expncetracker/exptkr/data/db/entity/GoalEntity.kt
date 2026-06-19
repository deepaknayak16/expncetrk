package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val iconName: String = "SAVINGS",
    val color: Int,
    val isCompleted: Boolean = false,

    // NEW: Which category auto-contributes to this goal
    @ColumnInfo(name = "linked_category")
    val linkedCategory: String? = null, // e.g. "Savings", "Investment"

    // NEW: Which account holds the "saved" money for this goal
    @ColumnInfo(name = "linked_account_id")
    val linkedAccountId: Long? = null
)
