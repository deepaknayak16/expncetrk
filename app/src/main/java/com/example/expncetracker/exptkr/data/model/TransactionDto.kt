package com.example.expncetracker.exptkr.data.model

import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Long = 0,
    val smsId: Long? = null,
    val amount: Double,
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
    val tags: List<String> = emptyList()
)

fun TransactionDto.toDomain() = Transaction(
    id = id,
    smsId = smsId,
    amount = amount,
    type = type,
    categoryName = category,
    merchant = merchant,
    bankName = bankName,
    note = note,
    timestamp = timestamp.toLocalDateTime(),
    isRecurring = isRecurring,
    frequency = frequency?.let { RecurrenceFrequency.valueOf(it) },
    nextDueDate = nextDueDate?.toLocalDateTime(),
    recurrenceEndDate = recurrenceEndDate?.toLocalDateTime(),
    parentTransactionId = parentTransactionId,
    counterparty = counterparty,
    isSettled = isSettled,
    tags = tags
)

fun Transaction.toDto() = TransactionDto(
    id = id,
    smsId = smsId,
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
    tags = tags
)
