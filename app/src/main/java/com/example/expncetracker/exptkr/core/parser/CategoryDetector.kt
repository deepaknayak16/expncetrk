package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.domain.usecase.ClassifyTransactionUseCase
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

@Singleton
class CategoryDetector @Inject constructor(
    private val repository: TransactionRepository,
    private val merchantMappingDao: MerchantMappingDao,
    private val classifyTransactionUseCase: ClassifyTransactionUseCase
) {
    private var cachedHistory: List<Transaction>? = null
    private var historyCacheTimestamp: Long = 0

    /**
     * Smartly detects the category for a given merchant.
     * Uses a multi-stage approach:
     * 1. Exact Merchant Mapping (User corrections)
     * 2. Exact History Match (Immediate learning)
     * 3. Naive Bayes ML Classifier (Trained on user history)
     * 4. Rule-based Fallback (Database patterns)
     */
    suspend fun detect(
        merchant: String,
        type: TransactionType,
        history: List<Transaction>? = null
    ): String {
        val cleanMerchant = merchant.trim()

        // 1. MERCHANT MAPPING (User's specific rules)
        val mapping = merchantMappingDao.getMappingForMerchant(cleanMerchant)
        if (mapping != null) {
            return mapping.categoryName
        }

        // Resolve history from repository if not provided by caller
        val resolvedHistory = history ?: getCachedHistory()

        // 2. EXACT HISTORY MATCH
        val exactMatch = resolvedHistory.find { it.merchant.equals(cleanMerchant, ignoreCase = true) && it.type == type }
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

        // 4. FALLBACK: Database Rules (Clean Architecture via Use Case)
        val matchedCategory = classifyTransactionUseCase(cleanMerchant, type)
        if (matchedCategory != null) {
            return matchedCategory
        }

        return "Others"
    }

    private suspend fun getCachedHistory(): List<Transaction> {
        val now = System.currentTimeMillis()
        if (cachedHistory == null || now - historyCacheTimestamp > 60_000) {
            cachedHistory = repository.getRecentTransactions(100).first()
            historyCacheTimestamp = now
        }
        return cachedHistory ?: emptyList()
    }

    private fun classifyMerchant(merchant: String, history: List<Transaction>): Prediction {
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
            val expProbs = probabilities.values.map { v ->
                kotlin.math.exp((v - maxLog).coerceAtMost(500.0)) // Cap to prevent overflow
            }
            val sumExp = expProbs.sum()
            if (sumExp == 0.0 || sumExp.isNaN()) return Prediction("Others", 0.0)
            val sortedProbs = expProbs.map { it / sumExp }.sortedDescending()
            
            // Confidence is the margin between the best and second-best probability
            if (sortedProbs.size >= 2) (sortedProbs[0] - sortedProbs[1]) else 1.0
        } else if (probabilities.size == 1) 1.0 else 0.0

        return Prediction(bestCategory, confidence)
    }

    private val tokenizeregex = Regex("[^a-zA-Z0-9]+")
    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .split(tokenizeregex)
            .filter { it.length > 2 }
    }

    private data class Prediction(val category: String, val confidence: Double)
}
