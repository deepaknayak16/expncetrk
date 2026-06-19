package com.example.expncetracker.exptkr.data.mapper

import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    smsId = smsId,
    amount = amount,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.DEBIT),
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
    tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    createdAt = createdAt.toLocalDateTime(),

)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    smsId = smsId,
    amount = amount,
    type = type.name,
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
    tags = if (tags.isEmpty()) null else tags.joinToString(","),
    createdAt = createdAt.toEpochMilli(),

)
