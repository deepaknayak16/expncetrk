package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val iconName: String, // To map back to an icon
    val color: Int
)
