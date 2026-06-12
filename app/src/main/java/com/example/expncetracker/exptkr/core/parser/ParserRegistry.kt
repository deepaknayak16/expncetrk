package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.core.parser.bank.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserRegistry @Inject constructor(
    private val parsers: List<BankParser>
) {
    private val genericParser = GenericParser()

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms? {
        val norm = sender.uppercase()
        
        // Find a specific parser that matches the sender
        val specificParser = parsers.firstOrNull { parser ->
            // This assumes BaseBankParser or similar has a way to identify its bank
            // For now we'll check against a map or similar, or just try them.
            // A better way is to have BankParser return its bank name or match criteria.
            // Let's refine this.
            norm.contains(getBankKey(parser))
        }
        
        return if (specificParser != null) {
            specificParser.parse(body, timestamp) 
                ?: genericParser.parse(body, timestamp)?.copy(bankName = getBankKey(specificParser))
        } else {
            genericParser.parse(body, timestamp)?.copy(bankName = sender)
        }
    }

    private fun getBankKey(parser: BankParser): String {
        return when (parser) {
            is HdfcParser -> "HDFC"
            is SbiParser -> "SBI"
            is IciciParser -> "ICICI"
            is AxisParser -> "AXIS"
            else -> ""
        }
    }
}
