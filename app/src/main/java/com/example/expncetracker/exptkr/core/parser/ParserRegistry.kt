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
            norm.contains(parser.bankKey)
        }
        
        return if (specificParser != null) {
            specificParser.parse(body, timestamp) 
                ?: genericParser.parse(body, timestamp)?.copy(bankName = specificParser.bankKey)
        } else {
            genericParser.parse(body, timestamp)?.copy(bankName = sender)
        }
    }
}
