package com.example.expncetracker.exptkr.core.parser.bank

class SbiParser : BaseBankParser("SBI") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)".toRegex(RegexOption.IGNORE_CASE)
    // FIX BUG-014: Use word boundary for Info to avoid capturing it
    override val merchantRegex = "(?i)(?:to|at|VPA|Info)\\b[:/\\s]+([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?=\\s+Ref|\\s+RefNo|\\s+\\bon\\b|\\.|$)".toRegex()
}
