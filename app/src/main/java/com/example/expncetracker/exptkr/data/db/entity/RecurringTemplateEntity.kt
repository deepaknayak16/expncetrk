package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val cleanMerchantName: String,
    val amount: BigDecimal,
    val category: String,
    val frequency: String, // e.g. "MONTHLY"
    val nextDueDate: Long,
    val state: String = "ACTIVE",
    val lastDetectedDate: Long,
    val confidenceScore: Float
)
