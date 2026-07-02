package com.example.expncetracker.exptkr.core.parser

data class CategorizationResult(val categorySlug: String, val entityName: String)

interface TransactionCategorizer {
    suspend fun findMatch(smsBody: String): CategorizationResult
}
