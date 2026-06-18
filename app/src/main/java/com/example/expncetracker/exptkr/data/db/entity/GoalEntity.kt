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
    // app/src/main/java/com/example/expncetracker/exptkr/data/db/entity/GoalEntity.kt
    @ColumnInfo(name = "linked_category")
    val linkedCategory: String? = null,   // e.g. "Savings", "Investment"
)
