package com.example.expncetracker.exptkr.domain.model

enum class DateFilter(val title: String) {
    DAY("Day"),
    WEEK("Week"),
    WEEK_RANGE("Week Range"),
    MONTH("Month"),
    YEAR("Year");

    fun toMonths(): Int = when (this) {
        DAY -> 1
        WEEK -> 1
        WEEK_RANGE -> 1
        MONTH -> 1
        YEAR -> 12
    }

    fun toDays(): Int = when (this) {
        DAY -> 1
        WEEK -> 7
        WEEK_RANGE -> 7
        MONTH -> 30
        YEAR -> 365
    }
}
