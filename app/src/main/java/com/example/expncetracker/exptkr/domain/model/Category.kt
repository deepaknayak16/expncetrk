package com.example.expncetracker.exptkr.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val displayName: String) {
    FOOD("Food"),
    CABS("Cabs"),
    RENT("Rent"),
    BILLS("Bills & Utilities"),
    SHOPPING("Shopping"),
    SALARY("Salary"),
    INVESTMENTS("Investments"),
    TRAVEL("Travel"),
    ENTERTAINMENT("Entertainment"),
    GROCERIES("Groceries"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    OTHERS("Others")
}
