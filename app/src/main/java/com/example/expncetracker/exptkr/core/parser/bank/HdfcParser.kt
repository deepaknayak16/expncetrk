package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)".toRegex(RegexOption.IGNORE_CASE)
    
    // FIX BUG REG-02: Use more specific capture group for VPAs and add lookahead for "(UPI" reference numbers
    override val merchantRegex = "(?i)(?:To|Paid to|VPA|at|towards|INFO)[:*]?\\s+(?!HDFC Bank|Bank A/c)([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?=\\s+\\bOn\\b\\s+\\d|\\s+\\bRef\\b|\\s+\\bRefNo\\b|\\s*\\(UPI|\\.|$)".toRegex()
    
    // Pattern 2: Specific fallback for UPI and "from VPA" formats
    // Added "from" and restricted capture to VPA-valid characters
    override val secondaryMerchantRegex = "(?i)(?:To|VPA|from)\\s+([^\\s\\d][^\\.\\s]+(?:\\s+[^\\s\\d][^\\.\\s]+)*?)(?=\\s+\\bOn\\b\\s+\\d|\\s+\\bRef\\b|\\s+\\bRefNo\\b|\\s*\\(UPI|\\.|$)".toRegex()
}
