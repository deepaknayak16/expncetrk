package com.example.expncetracker.exptkr.core.parser.bank

class AxisParser : BaseBankParser("AXIS") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))"
    )
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|Info:?|VPA[:/])\\s*([^.]+?)(?:Ref|RefNo|\\.|\$|on )".toRegex(RegexOption.IGNORE_CASE)
}
