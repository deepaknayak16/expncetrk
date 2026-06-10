package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

class GenericParser(private val bankName: String = "Bank") : BankParser {
    
    private val amountPattern = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)"
    private val debitKeywords = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)"
    private val creditKeywords = "(?:credited|deposited|received|added)"

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace("\n", " ")

        if (cleanBody.contains(debitKeywords.toRegex(RegexOption.IGNORE_CASE))) {
            amountPattern.toRegex(RegexOption.IGNORE_CASE).find(cleanBody)?.let { match ->
                val amt = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                return ParsedSms(amt, TransactionType.DEBIT, "Transaction", bankName, time)
            }
        }
        
        if (cleanBody.contains(creditKeywords.toRegex(RegexOption.IGNORE_CASE))) {
            amountPattern.toRegex(RegexOption.IGNORE_CASE).find(cleanBody)?.let { match ->
                val amt = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                return ParsedSms(amt, TransactionType.CREDIT, "Transaction", bankName, time)
            }
        }
        
        return null
    }
}
