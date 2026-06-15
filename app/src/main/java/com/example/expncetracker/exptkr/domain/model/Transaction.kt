package com.example.expncetracker.exptkr.domain.model

import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val smsId: Long? = null,
    val amount: Double,
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
    val tags: List<String> = emptyList()
) {
    init {
        require(amount >= 0) { "Amount cannot be negative" }
        require(merchant.isNotBlank()) { "Merchant name cannot be blank" }
        require(bankName.isNotBlank()) { "Bank name cannot be blank" }
        require(categoryName.isNotBlank()) { "Category name cannot be blank" }
    }
}
