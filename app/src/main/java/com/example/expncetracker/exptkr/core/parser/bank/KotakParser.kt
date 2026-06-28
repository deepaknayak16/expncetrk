package com.example.expncetracker.exptkr.core.parser.bank

class KotakParser : BaseBankParser("KOTAK") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "debited|spent|withdrawn|transferred|paid|sent".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?i)(?:to|at|VPA[:/]|INFO\\*|from)\\s+([^\\s\\d][^.\\s]+)".toRegex(RegexOption.IGNORE_CASE)
    override val secondaryMerchantRegex = "(?i)(?:to|at|VPA[:/])\\s+(.+?)(?=\\s+on|\\s+Ref|\\.|$)".toRegex(RegexOption.IGNORE_CASE)
}
