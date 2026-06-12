package com.example.expncetracker.exptkr.core.parser.bank

class AxisParser : BaseBankParser("AXIS") {
    override val amountRegex = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)".toRegex(RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|Info:?|VPA[:/])\\s*([^.]+?)(?:Ref|RefNo|\\.|\$|on )".toRegex(RegexOption.IGNORE_CASE)
}
