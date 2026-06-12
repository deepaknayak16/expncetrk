package com.example.expncetracker.exptkr.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    DEBIT, CREDIT, TRANSFER
}
