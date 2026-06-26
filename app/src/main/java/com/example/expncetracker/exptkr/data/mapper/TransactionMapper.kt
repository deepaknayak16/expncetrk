package com.example.expncetracker.exptkr.data.mapper

import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    smsId = smsId,
    accountId = accountId,
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
    tags = tags?.let { 
        if (it.startsWith("[")) runCatching { Json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        else it.split(",").filter { t -> t.isNotBlank() }
    } ?: emptyList(),
    createdAt = createdAt.toLocalDateTime(),
    idempotencyHash = idempotencyHash,
    confidenceScore = confidenceScore,
    parsingStatus = parsingStatus,
    isCategoryManuallyCorrected = isCategoryManuallyCorrected
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    smsId = smsId,
    accountId = accountId, // FIXED: was "if (accountId == 0L) null else accountId"
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
    tags = if (tags.isEmpty()) null else Json.encodeToString(tags),
    createdAt = createdAt.toEpochMilli(),
    idempotencyHash = idempotencyHash,
    confidenceScore = confidenceScore,
    parsingStatus = parsingStatus,
    isCategoryManuallyCorrected = isCategoryManuallyCorrected
)
