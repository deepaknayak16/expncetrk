package com.example.expncetracker.exptkr.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SampleDataDto(
    val version: String,
    val exportDate: String,
    val totalTransactions: Int,
    val transactions: List<SampleTransactionDto>
)

@Serializable
data class SampleTransactionDto(
    val id: Long,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String,
    val bankName: String,
    val date: Long
)
