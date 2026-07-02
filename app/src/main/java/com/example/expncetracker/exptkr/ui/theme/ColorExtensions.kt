package com.example.expncetracker.exptkr.ui.theme

import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor

/**
 * Safely converts a Hex String (e.g., "#FFF44336" or "#F44336") to a Compose Color.
 * Defaults to Color.Gray if the string is malformed.
 */
fun String?.toComposeColor(): Color {
    if (this.isNullOrBlank()) return Color.Gray
    return try {
        val sanitized = if (this.startsWith("#")) this else "#$this"
        Color(AndroidColor.parseColor(sanitized))
    } catch (e: Exception) {
        Color.Gray
    }
}
