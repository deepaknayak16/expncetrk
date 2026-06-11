package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

class AxisParser : BankParser {
    companion object {
        private val AMOUNT_REGEX = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)".toRegex(RegexOption.IGNORE_CASE)
        private val DEBIT_REGEX = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
        private val CREDIT_REGEX = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
        private val MERCHANT_REGEX = "(?:to|at|Info:?|VPA[:/])\\s*([^.]+?)(?:Ref|RefNo|\\.|\$|on )".toRegex(RegexOption.IGNORE_CASE)
    }

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        val time = timestamp.toLocalDateTime()
        val cleanBody = smsBody.replace("\n", " ")

        val isDebit = DEBIT_REGEX.containsMatchIn(cleanBody)
        val isCredit = CREDIT_REGEX.containsMatchIn(cleanBody)

        if (!isDebit && !isCredit) return null

        val amountMatch = AMOUNT_REGEX.find(cleanBody) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        
        val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT
        val merchant = MERCHANT_REGEX.find(cleanBody)?.groupValues?.get(1)?.trim() ?: if (isDebit) "Axis Debit" else "Axis Credit"

        return ParsedSms(amount, type, merchant, "AXIS", time)
    }
}
