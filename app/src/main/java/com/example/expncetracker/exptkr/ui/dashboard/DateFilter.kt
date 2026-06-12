package com.example.expncetracker.exptkr.ui.dashboard

enum class DateFilter(val title: String) {
    DAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year");

    fun toMonths(): Int = when (this) {
        DAY -> 1
        WEEK -> 1
        MONTH -> 1
        YEAR -> 12
    }
}
