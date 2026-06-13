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
    val timestamp: LocalDateTime
) {
    init {
        require(amount >= 0) { "Amount cannot be negative" }
        require(merchant.isNotBlank()) { "Merchant name cannot be blank" }
        require(bankName.isNotBlank()) { "Bank name cannot be blank" }
        require(categoryName.isNotBlank()) { "Category name cannot be blank" }
    }
}
