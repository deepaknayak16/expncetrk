package com.example.expncetracker.exptkr.core.common

import java.util.Locale

object MerchantNormalizer {
    /**
     * Consolidated merchant name normalization.
     * FIX BUG-GEN-04: Centralized logic.
     * FIX BUG-ML-13/11/12: Avoids generic fallbacks and handles truncation.
     */
    fun normalize(merchant: String): String {
        if (merchant.isBlank()) return "UNKNOWN"
        
        // Fix 5: Explicit concatenation and regex cleaning
        val raw = merchant.uppercase(Locale.ROOT)
            .replace(Regex("^[A-Z]{2}-"), "") // Strip bank prefixes like AX-, BZ-
            .replace(Regex("[^A-Z0-9 ]"), " ") // Keep alphanumeric and spaces
            .trim()
        
        val words = raw.split(Regex("\\s+"))
            .filter { it.length >= 2 } // Keep short meaningful words
        
        if (words.isEmpty()) return "UNKNOWN"
        
        // Special case: transport
        if (words[0] == "BMTC" || words[0] == "METRO") return words[0]
        
        // Heuristic: Prefer longest word or first two words
        val longestWord = words.maxByOrNull { it.length } ?: words[0]
        
        return if (words.size >= 2) {
            // If the first word is short (e.g. "SRI"), keep second word
            if (words[0].length <= 3) words[0] + " " + words[1]
            else words[0]
        } else {
            words[0]
        }
    }
}
