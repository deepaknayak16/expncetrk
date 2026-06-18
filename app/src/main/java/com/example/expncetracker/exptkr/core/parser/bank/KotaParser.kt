package com.example.expncetracker.exptkr.core.parser.bank

class KotaParser : BaseBankParser("KOTAK") {
    override val amountRegex: Regex = Regex(
        "(?:debited|credited|spent|paid|received|withdrawn)\\s*.*?[₹Rs]\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "debited|spent|withdrawn|transferred|paid|payment|sent".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "credited|deposited|received|added".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|VPA[:/]|INFO\\*)\\s+([^\\s\\d][^.\\s]+)".toRegex(RegexOption.IGNORE_CASE)
}
