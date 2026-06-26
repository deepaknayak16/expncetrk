package com.example.expncetracker.exptkr.ui.accounts

import java.math.BigDecimal

// FIX #5: Screen references this but file was missing
data class AccountUiModel(
    val id: Long,
    val name: String,
    val balance: BigDecimal,
    val type: String,
    val color: Int
)
