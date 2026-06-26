package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.core.common.Logger
import com.example.expncetracker.exptkr.core.parser.bank.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserRegistry @Inject constructor(
    private val parsers: List<BankParser>
) {
    private val genericParser = GenericParser()

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms? {
        // FIX #H9: Normalize sender to strip AD-/IM- prefixes for better account matching
        val cleanSender = sender.uppercase().replace(Regex("^[A-Z]{2}-"), "")
        val norm = cleanSender
        
        Logger.d("ParserRegistry", "Attempting to parse SMS from $sender (normalized: $cleanSender): ${body.take(30)}...")

        // Find a specific parser that matches the sender
        val specificParser = parsers.firstOrNull { parser ->
            norm.contains(parser.bankKey)
        }
        
        val result = if (specificParser != null) {
            Logger.d("ParserRegistry", "Found specific parser: ${specificParser.bankKey}")
            specificParser.parse(body, timestamp) 
                ?: genericParser.parse(body, timestamp)?.copy(bankName = specificParser.bankKey)
        } else {
            Logger.d("ParserRegistry", "No specific parser found, trying generic")
            genericParser.parse(body, timestamp)?.copy(bankName = cleanSender)
        }

        if (result != null) {
            Logger.d("ParserRegistry", "Successfully parsed: Amt=${result.amount}, Merchant=${result.merchant}")
        } else {
            Logger.d("ParserRegistry", "Failed to parse SMS from $sender")
        }

        return result
    }
}
