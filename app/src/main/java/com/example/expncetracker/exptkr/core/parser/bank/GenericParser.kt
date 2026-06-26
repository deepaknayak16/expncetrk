package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

class GenericParser(private val bankName: String = "Bank") : BankParser {
    override val bankKey: String = "GENERIC"
    
    private val debitKeywords = "debited|spent|withdrawn|transferred|paid|sent"
    private val creditKeywords = "credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned"
    private val allKeywords = "$debitKeywords|$creditKeywords"
    private val amountPattern = "(?i)(?:($allKeywords)\\s*.*?(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,.]+)|(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,.]+)\\s*.*?($allKeywords))"

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace(Regex("\\s+"), " ").trim()

        val match = amountPattern.toRegex().find(cleanBody) ?: return null
        
        var actionKeyword = ""
        var amountStr = ""
        
        val groups = match.groupValues.drop(1).filter { it.isNotBlank() }
        if (groups.size >= 2) {
            val first = groups[0]
            val second = groups[1]
            if (first.any { it.isDigit() }) {
                amountStr = first
                actionKeyword = second
            } else {
                actionKeyword = first
                amountStr = second
            }
        } else {
            return null
        }

        val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: return null

        val type = when {
            actionKeyword.contains(debitKeywords.toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.DEBIT
            actionKeyword.contains(creditKeywords.toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.CREDIT
            else -> return null
        }

        return ParsedSms(amount, type, "Transaction", bankName, time)
    }
}
