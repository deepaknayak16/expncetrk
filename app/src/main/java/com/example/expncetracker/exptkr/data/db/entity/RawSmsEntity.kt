package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_sms")
data class RawSmsEntity(
    @PrimaryKey val smsId: String, // FIX #H8: Use content-derived hash (String) as PK
    val body: String,
    val address: String,
    val timestamp: Long,
    val parsingStatus: String = "PENDING"
)
