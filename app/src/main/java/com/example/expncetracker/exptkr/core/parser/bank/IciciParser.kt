package com.example.expncetracker.exptkr.core.parser.bank

class IciciParser : BaseBankParser("ICICI") {
    override val amountRegex: Regex = Regex(
        "(?:debited|credited|spent|paid|received|withdrawn|Sent)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|Info:?|VPA[:/]|Towards)\\s*([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?:\\s+Ref|\\s+RefNo|\\s+on|\\.)".toRegex(RegexOption.IGNORE_CASE)
}
