package com.example.expncetracker.exptkr.data.model

import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val merchant: String,
    val bankName: String,
    val note: String? = null,
    val timestamp: Long
)

fun TransactionDto.toDomain() = Transaction(
    amount = amount,
    type = type,
    categoryName = category,
    merchant = merchant,
    bankName = bankName,
    note = note,
    timestamp = timestamp.toLocalDateTime()
)

fun Transaction.toDto() = TransactionDto(
    amount = amount,
    type = type,
    category = categoryName,
    merchant = merchant,
    bankName = bankName,
    note = note,
    timestamp = timestamp.toEpochMilli()
)
