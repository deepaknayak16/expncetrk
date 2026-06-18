package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.BuildConfig
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

    override fun parse(smsBody: String, timestamp: Long): ParsedSms? {
        return try {
            val time = timestamp.toLocalDateTime()
            val cleanBody = smsBody.replace("\n", " ")

            val isDebit = debitRegex.containsMatchIn(cleanBody)
            val isCredit = creditRegex.containsMatchIn(cleanBody)

            if (isDebit && isCredit) {
                Logger.d("BankParser", "Ambiguous SMS (both debit+credit): $cleanBody")
                return null // Let GenericParser handle it
            }

            if (!isDebit && !isCredit) return null

            val amountMatch = amountRegex.find(cleanBody) ?: return null
            val amountStr = amountMatch.groupValues.getOrNull(1)?.replace(",", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null
            
            if (amount < 0) return null
            if (amount == 0.0) {
                Logger.d("BankParser", "Zero-amount transaction from $bankName: $smsBody")
                return null
            }

            val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

            val merchant = merchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
                ?: if (isDebit) "$bankName Debit" else "$bankName Credit"

            ParsedSms(amount, type, merchant, bankName, time)
        } catch (e: Exception) {
            Logger.e("BankParser", "Parse failed: ${e.message}", e)
            null
        }
    }
}
