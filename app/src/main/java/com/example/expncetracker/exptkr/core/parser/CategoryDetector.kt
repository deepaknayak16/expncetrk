package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryDetector @Inject constructor(
    private val repository: TransactionRepository
) {
    /**
     * Smartly detects the category for a given merchant.
     * Uses a multi-stage approach:
     * 1. Exact History Match (Learning from user)
     * 2. Fuzzy History Match (Keyword patterns in history)
     * 3. Rule-based Fallback (Hardcoded patterns)
     */
    suspend fun detect(merchant: String, type: TransactionType): String {
        val cleanMerchant = merchant.trim()
        val upperMerchant = cleanMerchant.uppercase(Locale.ROOT)
        
        // Fetch history once for analysis
        val history = repository.getAllTransactions().first()

        // 1. SMART LEARNING: Check user's history for EXACT merchant match
        val exactMatch = history.find { it.merchant.equals(cleanMerchant, ignoreCase = true) }
        if (exactMatch != null) {
            return exactMatch.categoryName
        }

        // 2. SMART LEARNING: Check if the merchant name exists as a substring in previous transactions
        // e.g. If "SWIGGY*123" was categorized as "Food", then "SWIGGY*456" will also be "Food"
        val fuzzyMatch = history.find { 
            val histUpper = it.merchant.uppercase(Locale.ROOT)
            upperMerchant.contains(histUpper) || histUpper.contains(upperMerchant)
        }
        if (fuzzyMatch != null) {
            return fuzzyMatch.categoryName
        }

        // 3. FALLBACK: Static Rule-based detection
        if (type == TransactionType.CREDIT) {
            if (upperMerchant.contains("SALARY") || upperMerchant.contains("WIPRO") || 
                upperMerchant.contains("INFOSYS") || upperMerchant.contains("PAYROLL") || 
                upperMerchant.contains("HCL") || upperMerchant.contains("TCS")) return Category.SALARY.displayName
        }

        val detectedCategory = when {
            containsAny(upperMerchant, "SWIGGY", "ZOMATO", "RESTAU", "FOOD", "CAFE", "HOTEL", "EATS") -> Category.FOOD
            containsAny(upperMerchant, "UBER", "OLA", "RAPIDO", "NAMA", "METRO", "TAXI") -> Category.CABS
            containsAny(upperMerchant, "RENT", "NOBROKER", "HOUSING") -> Category.RENT
            containsAny(upperMerchant, "BESCOM", "AIRTEL", "JIO", "RECHARGE", "BILL", "ELECTRICITY", "WATER", "GAS") -> Category.BILLS
            containsAny(upperMerchant, "AMAZON", "FLIPKART", "DMART", "BLINKIT", "INSTAMART", "SHOP", "STORE", "MYNTRA", "AJIO", "MALL") -> Category.SHOPPING
            containsAny(upperMerchant, "ZERODHA", "GROWW", "MUTUAL", "INVEST", "STOCK", "BROKER", "COIN") -> Category.INVESTMENTS
            containsAny(upperMerchant, "AIRLINES", "FLIGHT", "INDIGO", "RAILWAY", "IRCTC", "TRAVEL", "MAKEMYTRIP", "GOIBIBO") -> Category.TRAVEL
            containsAny(upperMerchant, "NETFLIX", "PRIME", "HOTSTAR", "CINEMA", "MOVIE", "BOOKMYSHOW", "PVR", "INOX") -> Category.ENTERTAINMENT
            containsAny(upperMerchant, "BIGBASKET", "ZEPTO", "GROCERY", "MILK", "RETAIL", "SUPERMARKET") -> Category.GROCERIES
            containsAny(upperMerchant, "HOSPITAL", "PHARMACY", "DR ", "CLINIC", "MEDPLUS", "APOLLO", "HEALTH") -> Category.HEALTHCARE
            containsAny(upperMerchant, "SCHOOL", "COLLEGE", "FEE", "UNIVERSITY", "UDEMY", "COURSERA", "EDUCATION") -> Category.EDUCATION
            else -> Category.OTHERS
        }
        
        return detectedCategory.displayName
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
