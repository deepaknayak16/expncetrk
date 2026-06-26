package com.example.expncetracker.exptkr.domain.model

import java.math.BigDecimal

data class SpendingTrend(
    val label: String, // e.g., "Jan", "Feb"
    val amount: BigDecimal
)
