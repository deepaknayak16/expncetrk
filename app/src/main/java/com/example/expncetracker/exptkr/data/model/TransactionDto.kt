package com.example.expncetracker.exptkr.data.model

import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val merchant: String,
    val bankName: String,
    val timestamp: Long
)

fun TransactionDto.toDomain() = Transaction(
    amount = amount,
    type = type,
    category = category,
    merchant = merchant,
    bankName = bankName,
    timestamp = timestamp.toLocalDateTime()
)

fun Transaction.toDto() = TransactionDto(
    amount = amount,
    type = type,
    category = category,
    merchant = merchant,
    bankName = bankName,
    timestamp = timestamp.toEpochMilli()
)
