package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.ClassificationRule
import com.example.expncetracker.exptkr.domain.model.MatchType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import com.example.expncetracker.exptkr.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassifyTransactionUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(merchantName: String, type: TransactionType? = null): String? {
        if (merchantName.isBlank()) return null

        // FIX BUG-GEN-01: Removed unstable cache. Fetch from Flow to ensure fresh rules.
        val rules = ruleRepository.getActiveRules().first()

        // Filter by type if provided
        val typeFilteredRules = if (type != null) {
            rules.filter { it.transactionType == null || it.transactionType == type.name }
        } else {
            rules
        }

        // Pass 1: Exact match (highest confidence)
        typeFilteredRules
            .filter { it.matchType == MatchType.EXACT }
            .firstOrNull { merchantName.equals(it.keyword, ignoreCase = true) }
            ?.let { return it.category }

        // Pass 2: Contains match
        typeFilteredRules
            .filter { it.matchType == MatchType.CONTAINS }
            .firstOrNull { merchantName.contains(it.keyword, ignoreCase = true) }
            ?.let { return it.category }

        return null
    }
}
