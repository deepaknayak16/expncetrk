package com.example.expncetracker.exptkr.domain.model

import androidx.compose.ui.graphics.vector.ImageVector
import java.math.BigDecimal

data class Account(
    val name: String,
    val balance: BigDecimal,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)
