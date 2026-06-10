package com.example.expncetracker.exptkr.domain.model

import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val smsId: Long? = null,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val merchant: String,
    val bankName: String,
    val timestamp: LocalDateTime
)
