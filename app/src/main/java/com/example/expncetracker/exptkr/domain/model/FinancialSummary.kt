package com.example.expncetracker.exptkr.domain.model

data class FinancialSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val budget: Double? = null,
    val categoryDistribution: Map<String, Double>
)
