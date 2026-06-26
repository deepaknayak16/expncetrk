package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey val merchantName: String, // COLLATE NOCASE is usually set in @ColumnInfo or Table create
    val categoryName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
