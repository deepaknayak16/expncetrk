package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_quarantine")
data class SmsQuarantineEntity(
    @PrimaryKey val smsId: String,
    val body: String,
    val address: String,
    val timestamp: Long,
    val errorReason: String?,
    val quarantineDate: Long = System.currentTimeMillis()
)
