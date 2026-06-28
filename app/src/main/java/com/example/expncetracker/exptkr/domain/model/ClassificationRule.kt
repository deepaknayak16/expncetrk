package com.example.expncetracker.exptkr.domain.model

data class ClassificationRule(
    val keyword: String,
    val category: String,
    val matchType: MatchType,
    val priority: Int = 0,
    val transactionType: String? = null
)
