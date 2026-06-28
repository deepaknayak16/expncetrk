package com.example.expncetracker.exptkr.domain.usecase

import com.example.expncetracker.exptkr.domain.model.ClassificationRule
import com.example.expncetracker.exptkr.domain.model.MatchType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import com.example.expncetracker.exptkr.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassifyTransactionUseCase @Inject constructor(
    private val ruleRepository: RuleRepository
) {
    private var cachedRules: List<ClassificationRule>? = null

    suspend operator fun invoke(merchantName: String, type: TransactionType? = null): String? {
        if (merchantName.isBlank()) return null

        val rules = cachedRules
            ?: ruleRepository.getActiveRulesList().also { cachedRules = it }

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

    fun invalidateCache() {
        cachedRules = null
    }
}
