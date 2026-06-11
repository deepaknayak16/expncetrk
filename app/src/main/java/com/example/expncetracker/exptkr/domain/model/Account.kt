package com.example.expncetracker.exptkr.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Account(
    val name: String,
    val balance: Double,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)
