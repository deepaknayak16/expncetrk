package com.example.expncetracker.exptkr.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val smsId: String? = null,
    val accountId: Long = 0,
    val amount: BigDecimal,
    val type: TransactionType,
    val categoryName: String,
    val merchant: String,
    val bankName: String,
    val note: String? = null,
    val timestamp: LocalDateTime,
    val isRecurring: Boolean = false,
    val frequency: RecurrenceFrequency? = null,
    val nextDueDate: LocalDateTime? = null,
    val recurrenceEndDate: LocalDateTime? = null,
    val parentTransactionId: Long? = null,
    val counterparty: String? = null,
    val isSettled: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Phase 1 Additions
    val idempotencyHash: String? = null,
    val confidenceScore: Float = 1.0f,
    val parsingStatus: String = "COMPLETE",
    val isCategoryManuallyCorrected: Boolean = false
)
