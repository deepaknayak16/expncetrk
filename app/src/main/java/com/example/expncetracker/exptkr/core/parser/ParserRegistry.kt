package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.core.parser.bank.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserRegistry @Inject constructor() {
    private val parsers: Map<String, BankParser> = mapOf(
        "HDFC" to HdfcParser(),
        "SBI" to SbiParser(),
        "ICICI" to IciciParser(),
        "AXIS" to AxisParser()
    )
    
    private val genericParser = GenericParser()

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms? {
        val norm = sender.uppercase()
        val key = parsers.keys.firstOrNull { norm.contains(it) }
        
        return if (key != null) {
            parsers[key]?.parse(body, timestamp) ?: genericParser.parse(body, timestamp)?.copy(bankName = key)
        } else {
            genericParser.parse(body, timestamp)?.copy(bankName = sender)
        }
    }
}
