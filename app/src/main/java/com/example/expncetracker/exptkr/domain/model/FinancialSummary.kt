package com.example.expncetracker.exptkr.domain.model

import java.math.BigDecimal

data class FinancialSummary(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val balance: BigDecimal,
    val totalLent: BigDecimal = BigDecimal.ZERO,
    val totalBorrowed: BigDecimal = BigDecimal.ZERO,
    val totalAssets: BigDecimal = BigDecimal.ZERO,
    val totalLiabilities: BigDecimal = BigDecimal.ZERO,
    val netWorth: BigDecimal = BigDecimal.ZERO,
    val spendableBalance: BigDecimal = BigDecimal.ZERO,
    val budget: BigDecimal? = null,
    val categoryDistribution: Map<String, BigDecimal>
)
