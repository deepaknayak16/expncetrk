package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.core.common.Logger

class GenericParser(private val bankName: String = "Bank") : BankParser {
    override val bankKey: String = "GENERIC"
    
    // Actual transactions (Past tense)
    private val debitKeywords = "debited|spent|withdrawn|transferred|paid|sent|deducted"
    private val creditKeywords = "credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned"
    
    // Intent / Reminders (Future tense or Status)
    private val intentKeywords = "will be deducted|generated|due on|statement for|contribution"
    
    // Blacklist for noise
    private val noiseKeywords = "expired|plan name|limit"

    private val allKeywords = "$debitKeywords|$creditKeywords|$intentKeywords"
    
    // Regex for amount, excluding words labeled as 'balance'
    private val amountPattern = "(?i)(?:($allKeywords)\\s*.*?(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,.]+)(?!.*?balance)|(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,.]+)\\s*.*?($allKeywords)(?!.*?balance))"
    private val amountRegex = amountPattern.toRegex()

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace(Regex("\\s+"), " ").trim()

        // 1. Check for noise (Blacklist)
        if (noiseKeywords.split("|").any { cleanBody.contains(it.toRegex(RegexOption.IGNORE_CASE)) }) {
            Logger.d("GenericParser", "Ignored noise SMS: $cleanBody")
            return null
        }

        val match = amountRegex.find(cleanBody) ?: return null
        
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
        } else if (groups.size == 1) {
            amountStr = groups[0]
            // If we don't have a keyword in the capture group, we'll try to find one in the whole body
        } else {
            return null
        }

        val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: return null
        
        // 2. Identify Intent vs Transaction
        val isIntent = intentKeywords.split("|").any { cleanBody.contains(it.toRegex(RegexOption.IGNORE_CASE)) }
        
        // 3. Identify Type
        val type = when {
            actionKeyword.contains(debitKeywords.toRegex(RegexOption.IGNORE_CASE)) || 
            actionKeyword.contains("generated|due|deducted".toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.DEBIT
            
            actionKeyword.contains(creditKeywords.toRegex(RegexOption.IGNORE_CASE)) ||
            actionKeyword.contains("contribution".toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.CREDIT
            
            // Broad search if actionKeyword was empty or ambiguous
            cleanBody.contains(debitKeywords.toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.DEBIT
            cleanBody.contains(creditKeywords.toRegex(RegexOption.IGNORE_CASE)) -> TransactionType.CREDIT
            else -> return null
        }

        return ParsedSms(
            amount = amount,
            type = type,
            merchant = "Transaction",
            bankName = bankName,
            timestamp = time,
            isIntentOnly = isIntent
        )
    }
}
