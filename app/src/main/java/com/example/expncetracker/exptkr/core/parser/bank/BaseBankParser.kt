package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

abstract class BaseBankParser(private val bankName: String) : BankParser {

    abstract val amountRegex: Regex
    abstract val debitRegex: Regex
    abstract val creditRegex: Regex
    abstract val merchantRegex: Regex?

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        return try {
            val time = timestamp.toLocalDateTime()
            val cleanBody = smsBody.replace("\n", " ")

            val isDebit = debitRegex.containsMatchIn(cleanBody)
            val isCredit = creditRegex.containsMatchIn(cleanBody)

            if (!isDebit && !isCredit) return null

            val amountMatch = amountRegex.find(cleanBody) ?: return null
            val amountStr = amountMatch.groupValues.getOrNull(1)?.replace(",", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null
            if (amount < 0) return null  // Allow 0.00 for logging purposes
            if (amount == 0.0 && BuildConfig.DEBUG) {
                Log.d("BankParser", "Zero-amount transaction from $bankName: $smsBody")
            }

            if (amount <= 0) return null

            val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

            val merchant = merchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
                ?: if (isDebit) "$bankName Debit" else "$bankName Credit"

            ParsedSms(amount, type, merchant, bankName, time)
        } catch (e: Exception) {
            // FIX #13: Removed android.util.Log call. Use Timber in debug if needed.
            null
        }
    }
}
