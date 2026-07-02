package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

import com.example.expncetracker.exptkr.core.common.Logger

abstract class BaseBankParser(private val bankName: String) : BankParser {
    override val bankKey: String = bankName.uppercase()

    abstract val amountRegex: Regex
    abstract val debitRegex: Regex
    abstract val creditRegex: Regex
    abstract val merchantRegex: Regex?
    
    // Optional: secondary regex for merchants in different formats
    open val secondaryMerchantRegex: Regex? = null

    // Optional: regex to extract specific account identifier (e.g. A/c *8503 or Bank AC 3382)
    // FIX BUG-ML-13: Refined to avoid capturing phone numbers. Must be preceded by account keyword.
    open val accountRegex: Regex = "(?i)(?:A/c|Account|Card|through|A/C|AC)[:\\s]+(?:\\w+\\s+){0,2}([*X]*\\d{3,4})\\b".toRegex()
    
    // Optional: regex to extract absolute balance if available in SMS
    open val balanceRegex: Regex = "(?i)(?:Bal|Balance|Avl Bal|Available Balance|Limit)[:\\s]*[Rr]s\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)".toRegex()

    // Intent detection keywords
    private val intentKeywords = "will be deducted|generated|due on|statement for|contribution".toRegex(RegexOption.IGNORE_CASE)

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        return try {
            val time = timestamp.toLocalDateTime()
            // Normalize all whitespace to a single space
            val cleanBody = smsBody.replace(Regex("\\s+"), " ").trim()

            Logger.d("BankParser", "Parsing SMS from $bankName: $cleanBody")

            val amountMatch = amountRegex.find(cleanBody) ?: return null
            
            var actionKeyword = ""
            var amountStr = ""
            
            // FIX BUG-GEN-07: Harder group extraction to avoid keyword/amount collision
            val groupValues = amountMatch.groupValues.drop(1)
            // If we have 4 groups from (kw).*(amt)|(amt).*(kw)
            if (groupValues.size >= 4) {
                if (groupValues[0].isNotBlank()) {
                    actionKeyword = groupValues[0]
                    amountStr = groupValues[1]
                } else if (groupValues[2].isNotBlank()) {
                    amountStr = groupValues[2]
                    actionKeyword = groupValues[3]
                }
            } else {
                val groups = groupValues.filter { it.isNotBlank() }
                if (groups.size >= 2) {
                    val first = groups[0]
                    val second = groups[1]
                    if (first.any { it.isDigit() } && !second.any { it.isDigit() }) {
                        amountStr = first
                        actionKeyword = second
                    } else if (second.any { it.isDigit() } && !first.any { it.isDigit() }) {
                        actionKeyword = first
                        amountStr = second
                    } else {
                        // Ambiguous: pick largest number as amount? Or just first.
                        amountStr = first
                        actionKeyword = second
                    }
                } else if (groups.size == 1) {
                    amountStr = groups[0]
                } else {
                    return null
                }
            }

            val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: return null
            
            if (amount <= java.math.BigDecimal.ZERO) return null

            // Determine type
            val isDebit = debitRegex.containsMatchIn(actionKeyword)
            val isCredit = creditRegex.containsMatchIn(actionKeyword)

            val type = when {
                isDebit && !isCredit -> TransactionType.DEBIT
                isCredit && !isDebit -> TransactionType.CREDIT
                else -> {
                    val broadDebit = debitRegex.containsMatchIn(cleanBody)
                    val broadCredit = creditRegex.containsMatchIn(cleanBody)
                    if (broadDebit && !broadCredit) TransactionType.DEBIT
                    else if (broadCredit && !broadDebit) TransactionType.CREDIT
                    else return null
                }
            }

            // FIX BUG-ML-12: Avoid generic fallback merchant. 
            // If regex fails to extract a real merchant, return null to quarantine.
            val merchant = merchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: secondaryMerchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: return null // NEW: No merchant = no transaction (Quarantine it)

            // Try to extract a specific account name (e.g. HDFC *8503 -> HDFC-8503)
            val rawSuffix = accountRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
            val digitsOnly = rawSuffix?.filter { it.isDigit() }?.takeLast(4)
            val specificBankName = if (digitsOnly != null) "${bankName.uppercase()}-$digitsOnly" else bankName.uppercase()
            
            val isIntent = intentKeywords.containsMatchIn(cleanBody)

            val absoluteBalance = balanceRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull()

            ParsedSms(amount, type, merchant, specificBankName, time, isIntent, absoluteBalance)
        } catch (e: Exception) {
            Logger.e("BankParser", "Parse failed: ${e.message}", e)
            null
        }
    }
}
