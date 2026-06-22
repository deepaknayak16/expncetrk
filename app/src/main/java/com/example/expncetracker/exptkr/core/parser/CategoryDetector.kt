package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ln

class CategoryDetector @Inject constructor(
    private val repository: TransactionRepository,
    private val merchantMappingDao: MerchantMappingDao
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
        history: List<com.example.expncetracker.exptkr.domain.model.Transaction> = emptyList()
    ): String {
        val cleanMerchant = merchant.trim()
        val upperMerchant = cleanMerchant.uppercase(Locale.ROOT)

        // 1. MERCHANT MAPPING (User's specific rules)
        val mapping = merchantMappingDao.getMappingForMerchant(cleanMerchant)
        if (mapping != null) {
            return mapping.categoryName
        }

        // 2. EXACT HISTORY MATCH
        val exactMatch = history.find { it.merchant.equals(cleanMerchant, ignoreCase = true) }
        if (exactMatch != null) {
            return exactMatch.categoryName
        }

        // 3. ML CLASSIFIER (Naive Bayes)
        if (history.size >= 10) { 
            val prediction = classifyMerchant(cleanMerchant, history)
            if (prediction.confidence > 0.8) {
                return prediction.category
            }
        }

        // 4. FALLBACK: Static Rules
        if (type == TransactionType.CREDIT) {
            if (containsAny(upperMerchant, "SALARY", "PAYROLL", "WIPRO", "INFOSYS", "TCS", "HCL"))
                return "Salary"
        }

        return when {
            containsAny(upperMerchant, "SWIGGY", "ZOMATO", "RESTAU", "FOOD", "CAFE", "HOTEL", "EATS", "KITCHEN") -> "Food"
            containsAny(upperMerchant, "UBER", "OLA", "RAPIDO", "NAMA", "METRO", "TAXI", "AUTO", "TRAIN") -> "Cabs"
            containsAny(upperMerchant, "RENT", "NOBROKER", "HOUSING", "PG ") -> "Rent"
            containsAny(upperMerchant, "BESCOM", "AIRTEL", "JIO", "RECHARGE", "BILL", "ELECTRICITY", "WATER", "GAS", "BSNL", "BROADBAND") -> "Bills"
            containsAny(upperMerchant, "AMAZON", "FLIPKART", "DMART", "BLINKIT", "INSTAMART", "SHOP", "STORE", "MYNTRA", "AJIO", "MALL", "FASHION") -> "Shopping"
            containsAny(upperMerchant, "ZERODHA", "GROWW", "MUTUAL", "INVEST", "STOCK", "BROKER", "COIN", "SIP", "FUNDS") -> "Investments"
            containsAny(upperMerchant, "AIRLINES", "FLIGHT", "INDIGO", "RAILWAY", "IRCTC", "TRAVEL", "MAKEMYTRIP", "GOIBIBO", "STAY", "BOOKING") -> "Travel"
            containsAny(upperMerchant, "NETFLIX", "PRIME", "HOTSTAR", "CINEMA", "MOVIE", "BOOKMYSHOW", "PVR", "INOX", "SONY", "SPOTIFY") -> "Entertainment"
            containsAny(upperMerchant, "BIGBASKET", "ZEPTO", "GROCERY", "MILK", "RETAIL", "SUPERMARKET", "PROVISION") -> "Groceries"
            containsAny(upperMerchant, "HOSPITAL", "PHARMACY", "DR ", "CLINIC", "MEDPLUS", "APOLLO", "HEALTH", "DIAGNOSTICS") -> "Healthcare"
            containsAny(upperMerchant, "SCHOOL", "COLLEGE", "FEE", "UNIVERSITY", "UDEMY", "COURSERA", "EDUCATION", "COURSE") -> "Education"
            else -> "Others"
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
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
            val sorted = probabilities.values.sortedDescending()
            val totalRange = sorted[0] - sorted.last()
            if (totalRange != 0.0) ((sorted[0] - sorted[1]) / totalRange).coerceIn(0.0, 1.0) else 0.0
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
