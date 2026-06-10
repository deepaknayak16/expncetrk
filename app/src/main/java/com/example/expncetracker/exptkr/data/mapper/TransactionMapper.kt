package com.example.expncetracker.exptkr.data.mapper

import com.example.expncetracker.exptkr.core.common.toEpochMilli
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.data.db.entity.TransactionEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    smsId = smsId,
    amount = amount,
    type = TransactionType.valueOf(type),
    category = Category.valueOf(category),
    merchant = merchant,
    bankName = bankName,
    timestamp = timestamp.toLocalDateTime()
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    smsId = smsId,
    amount = amount,
    type = type.name,
    category = category.name,
    merchant = merchant,
    bankName = bankName,
    timestamp = timestamp.toEpochMilli()
)
