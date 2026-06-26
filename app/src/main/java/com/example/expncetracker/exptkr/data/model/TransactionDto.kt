package com.example.expncetracker.exptkr.data.model

import com.example.expncetracker.exptkr.core.common.BigDecimalSerializer
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TransactionDto(
    val id: Long = 0,
    val smsId: String? = null,
    val accountId: Long = 0, // <-- ADDED
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val type: TransactionType,
    val category: String,
    val merchant: String,
    val bankName: String,
    val note: String? = null,
    val timestamp: Long,
    val isRecurring: Boolean = false,
    val frequency: String? = null,
    val nextDueDate: Long? = null,
    val recurrenceEndDate: Long? = null,
    val parentTransactionId: Long? = null,
    val counterparty: String? = null,
    val isSettled: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val idempotencyHash: String? = null,
    val confidenceScore: Float = 1.0f,
    val parsingStatus: String = "COMPLETE"
)

fun TransactionDto.toDomain() = Transaction(
    id = id,
    smsId = smsId,
    accountId = accountId, // <-- ADDED
    amount = amount,
    type = type,
    categoryName = category,
    merchant = merchant,
    bankName = bankName,
    note = note,
    timestamp = timestamp.toLocalDateTime(),
    isRecurring = isRecurring,
    frequency = frequency?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() },
    nextDueDate = nextDueDate?.toLocalDateTime(),
    recurrenceEndDate = recurrenceEndDate?.toLocalDateTime(),
    parentTransactionId = parentTransactionId,
    counterparty = counterparty,
    isSettled = isSettled,
    tags = tags,
    createdAt = createdAt.toLocalDateTime(),
    idempotencyHash = idempotencyHash,
    confidenceScore = confidenceScore,
    parsingStatus = parsingStatus
)

fun Transaction.toDto() = TransactionDto(
    id = id,
    smsId = smsId,
    accountId = accountId, // <-- ADDED
    amount = amount,
    type = type,
    category = categoryName,
    merchant = merchant,
    bankName = bankName,
    note = note,
    timestamp = timestamp.toEpochMilli(),
    isRecurring = isRecurring,
    frequency = frequency?.name,
    nextDueDate = nextDueDate?.toEpochMilli(),
    recurrenceEndDate = recurrenceEndDate?.toEpochMilli(),
    parentTransactionId = parentTransactionId,
    counterparty = counterparty,
    isSettled = isSettled,
    tags = tags,
    createdAt = createdAt.toEpochMilli(),
    idempotencyHash = idempotencyHash,
    confidenceScore = confidenceScore,
    parsingStatus = parsingStatus
)
