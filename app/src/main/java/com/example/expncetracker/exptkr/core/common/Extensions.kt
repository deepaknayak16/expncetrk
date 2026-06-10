package com.example.expncetracker.exptkr.core.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

fun Double.formatAsCurrency(): String {
    return String.format("₹%,.2f", this)
}
