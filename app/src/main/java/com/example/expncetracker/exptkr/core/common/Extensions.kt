package com.example.expncetracker.exptkr.core.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

fun LocalDateTime.toEpochMilli(): Long {
    return this.atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

fun LocalDateTime.formatToDisplay(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    return this.format(formatter)
}

fun BigDecimal.formatAsCurrency(): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        format.format(this)
    } catch (e: Exception) {
        String.format(Locale.US, "₹%,.2f", this)
    }
}

fun Double.formatAsCurrency(): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        format.format(this)
    } catch (e: Exception) {
        String.format(Locale.US, "₹%,.2f", this)
    }
}
