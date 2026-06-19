package com.example.expncetracker.exptkr.ui.accounts

// FIX #5: Screen references this but file was missing
data class AccountUiModel(
    val id: Long,
    val name: String,
    val balance: Double,
    val type: String,
    val color: Int
)
