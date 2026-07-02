package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.core.common.Logger

class GenericParser(private val bankName: String = "Bank") : BankParser {
    override val bankKey: String = "GENERIC"
    
    // Actual transactions (Past tense or noun form) - Fix BUG-2
    private val debitKeywords = "debited|debit|spent|withdrawn|transferred|paid|sent|deducted"
    private val creditKeywords = "credited|credit|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned"
    
    // Intent / Reminders (Future tense or Status)
    private val intentKeywords = "will be deducted|generated|due on|statement for|contribution|amount to be paid|bill for|bill generated"
    
    // Blacklist for noise
    private val noiseKeywords = "expired|plan name|limit"

    private val allKeywords = "$debitKeywords|$creditKeywords|$intentKeywords"
    
    // Regex for amount: match amount near action keyword, no balance exclusion lookahead (FIX NEW-02)
    private val amountPattern = "(?i)(?:($allKeywords)\\s*[^\\d]{0,20}?(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|Amt|Amount|₹)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*[^\\d]{0,20}?($allKeywords))"
    private val amountRegex = amountPattern.toRegex()

    // Extract account digits for BANK-XXXX format
    // FIX BUG-ML-13: Refined to avoid capturing phone numbers. Must be preceded by account keyword.
    private val accountRegex = "(?i)(?:A/c|Account|Card|through|A/C|AC)[:\\s]+(?:\\w+\\s+){0,2}([*X]*\\d{3,4})\\b".toRegex()

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
        
        val groupValues = match.groupValues.drop(1)
        val groups = groupValues.filter { it.isNotBlank() }
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

        // Try to extract merchant name
        // FIX 1 & 4: Allowed digits at start ([^\\.\\s] instead of [^\\s\\d]) and single-char tokens
        val merchantRegex = "(?i)(?:To|Paid to|at|towards|INFO|For|From|by|Received from)[:*]?\\s+([^\\.\\s]+(?:\\s+[^\\.\\s]+)*?)(?=\\s+\\bOn\\b\\s+\\d|\\s+\\bRef\\b|\\s+\\bRefNo\\b|\\s*\\(UPI|\\.|$)".toRegex()
        val merchantMatch = merchantRegex.find(cleanBody)
        var merchant = merchantMatch?.groupValues?.getOrNull(1)?.trim()
            ?: if (type == TransactionType.CREDIT && cleanBody.contains("from", ignoreCase = true)) {
                cleanBody.substringAfter("from").trim().substringBefore(" ").trim()
            } else if (cleanBody.contains("CREDIT", ignoreCase = true) || cleanBody.contains("credited", ignoreCase = true)) {
                bankName.uppercase()
            } else null

        if (merchant == null) {
            Logger.d("GenericParser", "Failed to extract merchant from: $cleanBody")
            return null
        }

        val balanceRegex = "(?i)(?:Bal|Balance|Avl Bal|Available Balance|Limit)[:\\s]*[Rr]s\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)".toRegex()
        val absoluteBalance = balanceRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull()

        val rawSuffix = accountRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
        val digitsOnly = rawSuffix?.filter { it.isDigit() }?.takeLast(4)
        val finalBankName = if (digitsOnly != null) "${bankName.uppercase()}-$digitsOnly" else bankName.uppercase()

        return ParsedSms(
            amount = amount,
            type = type,
            merchant = merchant,
            bankName = finalBankName,
            timestamp = time,
            isIntentOnly = isIntent,
            accountBalance = absoluteBalance
        )
    }
}
