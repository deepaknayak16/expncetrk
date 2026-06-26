package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class ParsedSms(
    val amount: BigDecimal,
    val type: TransactionType,
    val merchant: String,
    val bankName: String,
    val timestamp: LocalDateTime
)

interface BankParser {
    val bankKey: String
    fun parse(smsBody: String, timestamp: Long): ParsedSms?
}
