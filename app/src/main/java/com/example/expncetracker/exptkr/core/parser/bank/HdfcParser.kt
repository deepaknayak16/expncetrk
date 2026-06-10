package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

class HdfcParser : BankParser {
    private val amountPattern = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)"
    private val debitKeywords = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)"
    private val creditKeywords = "(?:credited|deposited|received|added)"

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace("\n", " ")

        if (cleanBody.contains(debitKeywords.toRegex(RegexOption.IGNORE_CASE))) {
            amountPattern.toRegex(RegexOption.IGNORE_CASE).find(cleanBody)?.let { match ->
                val amt = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                val merchant = extractMerchant(cleanBody) ?: "HDFC Debit"
                return ParsedSms(amt, TransactionType.DEBIT, merchant, "HDFC", time)
            }
        }
        
        if (cleanBody.contains(creditKeywords.toRegex(RegexOption.IGNORE_CASE))) {
            amountPattern.toRegex(RegexOption.IGNORE_CASE).find(cleanBody)?.let { match ->
                val amt = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                val merchant = extractMerchant(cleanBody) ?: "HDFC Credit"
                return ParsedSms(amt, TransactionType.CREDIT, merchant, "HDFC", time)
            }
        }
        
        return null
    }

    private fun extractMerchant(body: String): String? {
        val patterns = listOf(
            "(?:to|at|towards)\\s+([^\\s\\d][^\\s]+)",
            "INFO\\*([^\\s]+)"
        )
        for (p in patterns) {
            p.toRegex(RegexOption.IGNORE_CASE).find(body)?.let {
                return it.groupValues[1].trim().replace("[^a-zA-Z]".toRegex(), " ").trim().take(20)
            }
        }
        return null
    }
}
