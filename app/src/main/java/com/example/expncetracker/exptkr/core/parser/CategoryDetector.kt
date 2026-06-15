package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ln

class CategoryDetector @Inject constructor(
    private val repository: TransactionRepository
) {
    /**
     * Smartly detects the category for a given merchant.
     * Uses a multi-stage approach:
     * 1. Exact History Match (Immediate learning)
     * 2. Naive Bayes ML Classifier (Trained on user history)
     * 3. Rule-based Fallback (Hardcoded patterns)
     */
    suspend fun detect(merchant: String, type: TransactionType): String {
        val cleanMerchant = merchant.trim()
        val upperMerchant = cleanMerchant.uppercase(Locale.ROOT)
        
        val history = repository.getAllTransactions()
            .map { it.take(200) }
            .first()

        // 1. EXACT HISTORY MATCH
        val exactMatch = history.find { it.merchant.equals(cleanMerchant, ignoreCase = true) }
        if (exactMatch != null) {
            return exactMatch.categoryName
        }

        // 2. ML CLASSIFIER (Naive Bayes)
        if (history.size >= 10) { // Only use ML if we have enough training data
            val prediction = classifyMerchant(cleanMerchant, history)
            if (prediction.confidence > 0.8) {
                return prediction.category
            }
        }

        // 3. FALLBACK: Static Rules
        if (type == TransactionType.CREDIT) {
            if (containsAny(upperMerchant, "SALARY", "PAYROLL", "WIPRO", "INFOSYS", "TCS", "HCL")) 
                return Category.SALARY.displayName
        }

        val detectedCategory = when {
            containsAny(upperMerchant, "SWIGGY", "ZOMATO", "RESTAU", "FOOD", "CAFE", "HOTEL", "EATS", "KITCHEN") -> Category.FOOD
            containsAny(upperMerchant, "UBER", "OLA", "RAPIDO", "NAMA", "METRO", "TAXI", "AUTO", "TRAIN") -> Category.CABS
            containsAny(upperMerchant, "RENT", "NOBROKER", "HOUSING", "PG ") -> Category.RENT
            containsAny(upperMerchant, "BESCOM", "AIRTEL", "JIO", "RECHARGE", "BILL", "ELECTRICITY", "WATER", "GAS", "BSNL", "BROADBAND") -> Category.BILLS
            containsAny(upperMerchant, "AMAZON", "FLIPKART", "DMART", "BLINKIT", "INSTAMART", "SHOP", "STORE", "MYNTRA", "AJIO", "MALL", "FASHION") -> Category.SHOPPING
            containsAny(upperMerchant, "ZERODHA", "GROWW", "MUTUAL", "INVEST", "STOCK", "BROKER", "COIN", "SIP", "FUNDS") -> Category.INVESTMENTS
            containsAny(upperMerchant, "AIRLINES", "FLIGHT", "INDIGO", "RAILWAY", "IRCTC", "TRAVEL", "MAKEMYTRIP", "GOIBIBO", "STAY", "BOOKING") -> Category.TRAVEL
            containsAny(upperMerchant, "NETFLIX", "PRIME", "HOTSTAR", "CINEMA", "MOVIE", "BOOKMYSHOW", "PVR", "INOX", "SONY", "SPOTIFY") -> Category.ENTERTAINMENT
            containsAny(upperMerchant, "BIGBASKET", "ZEPTO", "GROCERY", "MILK", "RETAIL", "SUPERMARKET", "PROVISION") -> Category.GROCERIES
            containsAny(upperMerchant, "HOSPITAL", "PHARMACY", "DR ", "CLINIC", "MEDPLUS", "APOLLO", "HEALTH", "DIAGNOSTICS") -> Category.HEALTHCARE
            containsAny(upperMerchant, "SCHOOL", "COLLEGE", "FEE", "UNIVERSITY", "UDEMY", "COURSERA", "EDUCATION", "COURSE") -> Category.EDUCATION
            else -> Category.OTHERS
        }
        
        return detectedCategory.displayName
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    /**
     * Light-weight Naive Bayes Classifier trained on the fly using user data.
     */
    private fun classifyMerchant(merchant: String, history: List<com.example.expncetracker.exptkr.domain.model.Transaction>): Prediction {
        val tokens = tokenize(merchant)
        if (tokens.isEmpty()) return Prediction(Category.OTHERS.displayName, 0.0)

        val categories = history.map { it.categoryName }.distinct()
        val categoryCounts = history.groupingBy { it.categoryName }.eachCount()
        val totalDocs = history.size.toDouble()

        // word -> category -> count
        val wordCounts = mutableMapOf<String, MutableMap<String, Int>>()
        history.forEach { tx ->
            tokenize(tx.merchant).forEach { word ->
                val counts = wordCounts.getOrPut(word) { mutableMapOf() }
                counts[tx.categoryName] = (counts[tx.categoryName] ?: 0) + 1
            }
        }

        var bestCategory = Category.OTHERS.displayName
        var maxLogProb = Double.NEGATIVE_INFINITY
        val probabilities = mutableMapOf<String, Double>()

        categories.forEach { cat ->
            // Prior: P(cat)
            var logProb = ln(((categoryCounts[cat] ?: 0) + 1).toDouble() / (totalDocs + categories.size))
            
            // Vocabulary size for Laplace smoothing
            val vocabSize = wordCounts.size
            val totalWordsInCat = wordCounts.values.sumOf { it[cat] ?: 0 }

            tokens.forEach { token ->
                // Likelihood: P(token|cat) with Laplace smoothing
                val count = (wordCounts[token]?.get(cat) ?: 0) + 1
                logProb += ln(count.toDouble() / (totalWordsInCat + vocabSize))
            }

            if (logProb > maxLogProb) {
                maxLogProb = logProb
                bestCategory = cat
            }
            probabilities[cat] = logProb
        }

        // Calculate confidence by comparing the best log-prob to the average
        // This is a simplified heuristic for confidence
        val confidence = if (probabilities.size > 1) {
            val sorted = probabilities.values.sortedDescending()
            val diff = sorted[0] - sorted[1]
            (diff / -sorted[0]).coerceIn(0.0, 1.0)
        } else 1.0

        return Prediction(bestCategory, confidence)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 }
    }

    private data class Prediction(val category: String, val confidence: Double)
}
