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
        val upperBody = body.uppercase()

        // FIX 4: EPFO / PF Balance Trap
        // These are NOT debits/credits from the user's bank account. They are informational.
        if (upperBody.contains("PASSBOOK BALANCE")) {
            Logger.d("ParserRegistry", "Ignored EPFO informational SMS (Passbook Balance)")
            return null 
        }

        // FIX 4: Airtel "Bill Generated" Trap
        // "Amount to be paid: Rs 411.82" -> This is a future liability, not a past transaction.
        if (upperBody.contains("AMOUNT TO BE PAID") || upperBody.contains("BILL GENERATED")) {
            Logger.d("ParserRegistry", "Ignored future liability bill generation SMS")
            return null 
        }

        // ADDITIONAL FIX: Ignore "payment received" confirmations for mobile/broadband bills (not income)
        if (upperBody.contains("WE HAVE RECEIVED PAYMENT") && (upperBody.contains("MOBILE") || upperBody.contains("AIRTEL") || upperBody.contains("BROADBAND"))) {
            Logger.d("ParserRegistry", "Ignored bill payment confirmation (Not income)")
            return null
        }

        // Aggressive normalization to group merchants like AX-EPFOHO and BZ-EPFOHO
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
