package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val limitAmount: java.math.BigDecimal,
    val lastAlertSentAt: Long? = null
)
