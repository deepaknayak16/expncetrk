package com.example.expncetracker.exptkr.core.parser.bank

import com.example.expncetracker.exptkr.core.parser.BankParser
import com.example.expncetracker.exptkr.core.parser.ParsedSms
import com.example.expncetracker.exptkr.core.common.toLocalDateTime
import com.example.expncetracker.exptkr.domain.model.TransactionType

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)".toRegex(RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|towards|INFO\\*)\\s+([^\\s\\d][^\\.\\s]+)".toRegex(RegexOption.IGNORE_CASE)
}
