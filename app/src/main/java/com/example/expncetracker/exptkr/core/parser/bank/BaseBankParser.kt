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

    // Optional: regex to extract specific account identifier (e.g. A/c *8503)
    open val accountRegex: Regex = "(?i)(?:A/c|Account|Card|from|through|A/C)[:\\s]+([*X]*\\d{3,})".toRegex()

    // Intent detection keywords
    private val intentKeywords = "will be deducted|generated|due on|statement for|contribution".toRegex(RegexOption.IGNORE_CASE)

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        return try {
            val time = timestamp.toLocalDateTime()
            // Normalize all whitespace (including newlines, tabs, and multiple spaces) to a single space
            val cleanBody = smsBody.replace(Regex("\\s+"), " ").trim()

            Logger.d("BankParser", "Parsing SMS from $bankName: $cleanBody")

            // Action keyword and amount can be in different orders
            val amountMatch = amountRegex.find(cleanBody) ?: return null
            
            // Flexible group extraction: find the first non-empty group for keyword and amount
            // Assuming amountRegex is structured as (keyword).*(amount) OR (amount).*(keyword)
            var actionKeyword = ""
            var amountStr = ""
            
            // We'll look for groups that are not the full match (index 0) and not blank
            val groups = amountMatch.groupValues.drop(1).filter { it.isNotBlank() }
            if (groups.size >= 2) {
                // We need to know which is which. 
                // Let's assume the one containing digits is the amount.
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
                // Only amount captured? Fallback to broad check
                amountStr = groups[0]
            } else {
                return null
            }

            val amount = amountStr.replace(",", "").toBigDecimalOrNull() ?: return null
            
            if (amount <= java.math.BigDecimal.ZERO) {
                Logger.d("BankParser", "Zero or negative-amount transaction from $bankName: $smsBody")
                return null
            }

            // Determine type based on the action keyword nearest to the amount
            val isDebit = debitRegex.containsMatchIn(actionKeyword)
            val isCredit = creditRegex.containsMatchIn(actionKeyword)

            val type = when {
                isDebit && !isCredit -> TransactionType.DEBIT
                isCredit && !isDebit -> TransactionType.CREDIT
                else -> {
                    // Fallback to broad check if the immediate keyword is ambiguous or missing
                    val broadDebit = debitRegex.containsMatchIn(cleanBody)
                    val broadCredit = creditRegex.containsMatchIn(cleanBody)
                    if (broadDebit && !broadCredit) TransactionType.DEBIT
                    else if (broadCredit && !broadDebit) TransactionType.CREDIT
                    else {
                        Logger.d("BankParser", "Ambiguous or unknown transaction type for $bankName: $cleanBody")
                        return null
                    }
                }
            }

            val merchant = merchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: secondaryMerchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: if (type == TransactionType.DEBIT) "$bankName Debit" else "$bankName Credit"

            // Try to extract a specific account name (e.g. HDFC *8503)
            val accountSuffix = accountRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
            val specificBankName = if (accountSuffix != null) "$bankName $accountSuffix" else bankName
            
            val isIntent = intentKeywords.containsMatchIn(cleanBody)

            ParsedSms(amount, type, merchant, specificBankName, time, isIntent)
        } catch (e: Exception) {
            Logger.e("BankParser", "Parse failed: ${e.message}", e)
            null
        }
    }
}
