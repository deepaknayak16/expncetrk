package com.example.expncetracker.exptkr.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
@Keep
@Serializable
enum class TransactionType {
    DEBIT, CREDIT, TRANSFER, LEND, BORROW
}
