package com.example.expncetracker.exptkr.domain.model

data class FinancialSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val categoryDistribution: Map<Category, Double>
)
