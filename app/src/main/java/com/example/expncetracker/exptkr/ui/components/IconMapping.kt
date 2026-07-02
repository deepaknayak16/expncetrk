package com.example.expncetracker.exptkr.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

val availableIcons = listOf(
    "FOOD" to Icons.Default.Restaurant,
    "CABS" to Icons.Default.DirectionsCar,
    "RENT" to Icons.Default.HomeWork,
    "BILLS" to Icons.Default.Bolt,
    "SHOPPING" to Icons.Default.LocalMall,
    "SALARY" to Icons.Default.Payments,
    "INVESTMENTS" to Icons.AutoMirrored.Filled.TrendingUp,
    "TRAVEL" to Icons.Default.LocalAirport,
    "ENTERTAINMENT" to Icons.Default.LiveTv,
    "HEALTHCARE" to Icons.Default.Favorite,
    "EDUCATION" to Icons.Default.School,
    "GROCERIES" to Icons.Default.ShoppingCart,
    "OTHERS" to Icons.Default.GridView
)

fun getIconByName(name: String): ImageVector {
    return availableIcons.find { it.first.equals(name, ignoreCase = true) }?.second ?: Icons.Default.GridView
}

val presetColors = listOf(
    0xFFF97316, // Orange
    0xFF6366F1, // Indigo
    0xFFEC4899, // Pink
    0xFFA855F7, // Purple
    0xFF10B981, // Emerald
    0xFFEAB308, // Yellow
    0xFF06B6D4, // Cyan
    0xFF3B82F6, // Blue
    0xFF8B5CF6, // Violet
    0xFFEF4444, // Red
    0xFF14B8A6, // Teal
    0xFF4ADE80, // Green
    0xFF64748B  // Slate
)
