package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.core.common.toLocalDateTime

class DopBankParser : BaseBankParser("India Post Bank") {
    override val bankKey: String = "DOPBNK"
    
    override val amountRegex: Regex = Regex(
        "(?i)(?:amount|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE
    )
    
    override val debitRegex = "DEBITED|DEBIT|WITHDRAWAL".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "CREDIT|CREDITED|DEPOSITED".toRegex(RegexOption.IGNORE_CASE)
    
    // For DOPBNK, if it's a credit without a specific "from", we use "Interest/Deposit"
    override val merchantRegex = "(?i)(?:To|at|Towards)\\s+([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?=\\s+\\bOn\\b|\\.|$)".toRegex()
    
    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val base = super.parse(smsBody, timestamp)
        if (base != null) return base
        
        // Custom fallback for DOPBNK specific formats that super.parse might reject due to missing merchant
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace(Regex("\\s+"), " ").trim()
        
        val amountMatch = amountRegex.find(cleanBody) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        
        val isCredit = creditRegex.containsMatchIn(cleanBody)
        val isDebit = debitRegex.containsMatchIn(cleanBody)
        
        val type = when {
            isCredit && !isDebit -> TransactionType.CREDIT
            isDebit && !isCredit -> TransactionType.DEBIT
            else -> return null
        }
        
        // If we reached here, super.parse failed (likely due to merchantRegex)
        // For DOPBNK CREDITS, we default the merchant to "Post Office Savings"
        val merchant = if (type == TransactionType.CREDIT) "Post Office Savings" else "Bank"
        
        // Extract account digits for BANK-XXXX format (e.g. Account No. XXXXXXXX1199 -> DOPBNK-1199)
        val rawSuffix = accountRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
        val digitsOnly = rawSuffix?.filter { it.isDigit() }?.takeLast(4)
        val finalBankName = if (digitsOnly != null) "DOPBNK-$digitsOnly" else "DOPBNK"

        val absoluteBalance = balanceRegex.find(cleanBody)?.groupValues?.getOrNull(1)?.replace(",", "")?.toBigDecimalOrNull()

        return ParsedSms(amount, type, merchant, finalBankName, time, false, absoluteBalance)
    }
}
