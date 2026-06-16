package com.example.expncetracker.exptkr.core.parser.bank

import android.util.Log
import com.example.expncetracker.BuildConfig
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

            if (isDebit && isCredit) {
                if (BuildConfig.DEBUG) Log.w("BankParser", "Ambiguous SMS (both debit+credit): $cleanBody")
                return null // Let GenericParser handle it
            }

            if (!isDebit && !isCredit) return null

            val amountMatch = amountRegex.find(cleanBody) ?: return null
            val amountStr = amountMatch.groupValues.getOrNull(1)?.replace(",", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null
            
            if (amount < 0) return null
            if (amount == 0.0) {
                if (BuildConfig.DEBUG) Log.d("BankParser", "Zero-amount transaction from $bankName: $smsBody")
                return null
            }

            val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

            val merchant = merchantRegex?.find(cleanBody)?.groupValues?.getOrNull(1)?.trim()
                ?: if (isDebit) "$bankName Debit" else "$bankName Credit"

            ParsedSms(amount, type, merchant, bankName, time)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d("BankParser", "Parse failed: ${e.message}")
            null
        }
    }
}
