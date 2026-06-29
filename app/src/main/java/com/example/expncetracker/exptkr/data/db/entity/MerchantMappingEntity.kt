package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_mappings")
data class MerchantMappingEntity(
    @PrimaryKey 
    @ColumnInfo(name = "merchantName", collate = ColumnInfo.NOCASE)
    val merchantName: String,
    val categoryName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
