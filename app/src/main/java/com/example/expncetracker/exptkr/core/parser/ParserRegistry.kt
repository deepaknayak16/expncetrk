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

    // FIX BUG-ML-15: Map cryptic sender IDs to human-readable bank names
    private val senderMappings = mapOf(
        "HDFCBK" to "HDFC Bank",
        "ICICIB" to "ICICI Bank",
        "KOTAKB" to "Kotak Bank",
        "AXISBK" to "Axis Bank",
        "SBIBNK" to "SBI",
        "EPFOHO" to "EPFO",
        "DOPBNK" to "India Post Bank",
        "AIRBIL" to "Airtel"
    )

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms? {
        val upperBody = body.uppercase()

        // FIX 4: EPFO / PF Balance Trap (REFINED)
        // These are informational but we want them as REMINDERS if they mention Contribution
        if (upperBody.contains("PASSBOOK BALANCE") && !upperBody.contains("CONTRIBUTION")) {
            Logger.d("ParserRegistry", "Ignored purely informational Passbook Balance SMS")
            return null 
        }

        // ADDITIONAL FIX: Ignore "payment received" confirmations for mobile/broadband bills (not income)
        if (upperBody.contains("WE HAVE RECEIVED PAYMENT") && (upperBody.contains("MOBILE") || upperBody.contains("AIRTEL") || upperBody.contains("BROADBAND")) && !upperBody.contains("RS.") && !upperBody.contains("₹")) {
            Logger.d("ParserRegistry", "Ignored bill payment confirmation (No amount)")
            return null
        }

        // Aggressive normalization to group merchants like AX-EPFOHO and BZ-EPFOHO
        val cleanSender = sender.uppercase().replace(Regex("^[A-Z]{2}-"), "")
        val norm = cleanSender

        Logger.d("ParserRegistry", "Attempting to parse SMS from $sender (normalized: $cleanSender): ${body.take(30)}...")

        // FIX: Match by prefix to handle suffixes like -S, -G, -T (e.g., HDFCBK-S matches HDFC Bank)
        val mappedBankName = senderMappings.entries
            .filter { cleanSender.startsWith(it.key) }
            .maxByOrNull { it.key.length }?.value ?: cleanSender

        // Find a specific parser that matches the sender
        val specificParser = parsers.firstOrNull { parser ->
            norm.contains(parser.bankKey)
        }
        
        val result = if (specificParser != null) {
            Logger.d("ParserRegistry", "Found specific parser: ${specificParser.bankKey}")
            specificParser.parse(body, timestamp)
        } else {
            Logger.d("ParserRegistry", "No specific parser found, trying generic")
            genericParser.parse(body, timestamp)
        }

        // POST-PROCESSING: Ensure the bank name is clean and contains the mapped name + digits
        val finalResult = result?.let {
            val digits = it.bankName.substringAfter("-").takeIf { s -> s.all { c -> c.isDigit() } && s.length >= 3 }
            val baseName = if (mappedBankName.uppercase().contains(it.bankName.substringBefore("-").uppercase())) {
                mappedBankName
            } else {
                it.bankName.substringBefore("-")
            }
            
            it.copy(bankName = if (digits != null) "${baseName.uppercase()}-$digits" else baseName.uppercase())
        }

        if (finalResult != null) {
            Logger.d("ParserRegistry", "Successfully parsed: Amt=${finalResult.amount}, Merchant=${finalResult.merchant}, Bank=${finalResult.bankName}")
        } else {
            Logger.d("ParserRegistry", "Failed to parse SMS from $sender")
        }

        return finalResult
    }
}
