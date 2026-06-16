package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex = "(?:Rs|INR|Amt|Amount)\\.?\\s*([0-9,.]+)".toRegex(RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    override val merchantRegex = "(?:to|at|towards|INFO\\*)\\s+([^\\s\\d][^\\.\\s]+)".toRegex(RegexOption.IGNORE_CASE)
}
