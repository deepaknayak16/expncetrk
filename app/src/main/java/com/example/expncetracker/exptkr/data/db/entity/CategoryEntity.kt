package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Slug (e.g., "dining")
    val displayName: String = "", // Display Name (e.g., "Food & Dining")
    val type: String, // "INCOME" or "EXPENSE"
    val iconName: String, // To map back to an icon
    val color: Int
)
