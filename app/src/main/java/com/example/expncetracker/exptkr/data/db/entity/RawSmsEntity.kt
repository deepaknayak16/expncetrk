package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_sms")
data class RawSmsEntity(
    @PrimaryKey val smsId: Long,
    val body: String,
    val address: String,
    val timestamp: Long
)
