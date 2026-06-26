package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.ln

class CategoryDetector @Inject constructor(
    private val repository: TransactionRepository,
    private val merchantMappingDao: MerchantMappingDao,
    private val ruleRepository: RuleRepository
) {
    /**
     * Smartly detects the category for a given merchant.
     * Uses a multi-stage approach:
     * 1. Exact Merchant Mapping (User corrections)
     * 2. Exact History Match (Immediate learning)
     * 3. Naive Bayes ML Classifier (Trained on user history)
     * 4. Rule-based Fallback (Hardcoded patterns)
     */
    suspend fun detect(
        merchant: String,
        type: TransactionType,
        history: List<com.example.expncetracker.exptkr.domain.model.Transaction>? = null
    ): String {
        val cleanMerchant = merchant.trim()
        val upperMerchant = cleanMerchant.uppercase(Locale.ROOT)

        // 1. MERCHANT MAPPING (User's specific rules)
        val mapping = merchantMappingDao.getMappingForMerchant(cleanMerchant)
        if (mapping != null) {
            return mapping.categoryName
        }

        // Resolve history from repository if not provided by caller
        // FIX #H7: Only fetch recent history to avoid performance hit on large DBs
        val resolvedHistory = history ?: repository.getRecentTransactions(50).first()

        // 2. EXACT HISTORY MATCH
        val exactMatch = resolvedHistory.find { it.merchant.equals(cleanMerchant, ignoreCase = true) }
        if (exactMatch != null) {
            return exactMatch.categoryName
        }

        // 3. ML CLASSIFIER (Naive Bayes)
        if (resolvedHistory.size >= 10) { 
            val prediction = classifyMerchant(cleanMerchant, resolvedHistory)
            if (prediction.confidence > 0.8) {
                return prediction.category
            }
        }

        // 4. FALLBACK: Database Rules
        val dbRules = ruleRepository.getActiveRulesList()
        val matchedRule = dbRules.find { rule ->
            val typeMatches = rule.transactionType == null || rule.transactionType == type.name
            typeMatches && containsAny(upperMerchant, rule.pattern)
        }
        if (matchedRule != null) {
            return matchedRule.categoryName
        }

        return "Others"
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { kw ->
            val regex = Regex("\\b" + Regex.escape(kw) + "\\b", RegexOption.IGNORE_CASE)
            regex.containsMatchIn(text)
        }
    }

    private fun classifyMerchant(merchant: String, history: List<com.example.expncetracker.exptkr.domain.model.Transaction>): Prediction {
        val tokens = tokenize(merchant)
        if (tokens.isEmpty()) return Prediction("Others", 0.0)

        val categories = history.map { it.categoryName }.distinct()
        val categoryCounts = history.groupingBy { it.categoryName }.eachCount()
        val totalDocs = history.size.toDouble()

        val wordCounts = mutableMapOf<String, MutableMap<String, Int>>()
        history.forEach { tx ->
            tokenize(tx.merchant).forEach { word ->
                val counts = wordCounts.getOrPut(word) { mutableMapOf() }
                counts[tx.categoryName] = (counts[tx.categoryName] ?: 0) + 1
            }
        }

        var bestCategory = "Others"
        var maxLogProb = Double.NEGATIVE_INFINITY
        val probabilities = mutableMapOf<String, Double>()

        categories.forEach { cat ->
            var logProb = ln(((categoryCounts[cat] ?: 0) + 1).toDouble() / (totalDocs + categories.size))
            val vocabSize = wordCounts.size
            val totalWordsInCat = wordCounts.values.sumOf { it[cat] ?: 0 }

            tokens.forEach { token ->
                val count = (wordCounts[token]?.get(cat) ?: 0) + 1
                logProb += ln(count.toDouble() / (totalWordsInCat + vocabSize))
            }

            if (logProb > maxLogProb) {
                maxLogProb = logProb
                bestCategory = cat
            }
            probabilities[cat] = logProb
        }

        val confidence = if (probabilities.size > 1) {
            // Softmax conversion for real probabilities
            val maxLog = probabilities.values.maxOrNull() ?: 0.0
            val expProbs = probabilities.values.map { exp(it - maxLog) }
            val sumExp = expProbs.sum()
            val sortedProbs = expProbs.map { it / sumExp }.sortedDescending()
            
            // Confidence is the margin between the best and second-best probability
            if (sortedProbs.size >= 2) (sortedProbs[0] - sortedProbs[1]) else 1.0
        } else if (probabilities.size == 1) 1.0 else 0.0

        return Prediction(bestCategory, confidence)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 }
    }

    private data class Prediction(val category: String, val confidence: Double)
}
