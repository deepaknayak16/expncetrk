package com.example.expncetracker.exptkr.core.common

import java.util.Locale

object MerchantNormalizer {
    /**
     * Consolidated merchant name normalization.
     * FIX BUG REG-01: Removed aggressive truncation that was breaking multi-word rule matching.
     */
    fun normalize(merchant: String): String {
        if (merchant.isBlank()) return "UNKNOWN"
        
        // Clean special characters and bank prefixes
        val raw = merchant.uppercase(Locale.ROOT)
            .replace(Regex("^[A-Z]{2}-"), "") // Strip bank prefixes like AX-, BZ-
            .replace(Regex("[^A-Z0-9 ]"), " ") // Keep alphanumeric and spaces
            .trim()
        
        // Normalize multiple spaces to a single space
        val normalized = raw.replace(Regex("\\s+"), " ")
        
        if (normalized.isBlank()) return "UNKNOWN"
        
        // Special case: transport
        if (normalized.startsWith("BMTC") || normalized.startsWith("METRO")) {
            return normalized.split(" ").first()
        }
        
        return normalized
    }
}
