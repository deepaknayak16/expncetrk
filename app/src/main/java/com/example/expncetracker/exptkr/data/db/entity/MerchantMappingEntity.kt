package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey val merchantName: String,
    val categoryName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
