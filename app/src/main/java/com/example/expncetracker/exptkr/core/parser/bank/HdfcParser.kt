package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex: Regex = Regex(
        "(?:debited|credited|spent|paid|received|withdrawn|Sent)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|towards|INFO\\*)\\s+([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?:\\s+On\\s+|\\s+Ref|\\s+RefNo|\\.)".toRegex(RegexOption.IGNORE_CASE)
}
