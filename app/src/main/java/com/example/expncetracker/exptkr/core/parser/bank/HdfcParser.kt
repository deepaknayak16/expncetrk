package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)".toRegex(RegexOption.IGNORE_CASE)
    
    // FIX BUG-ML-13: Use negative lookahead to skip "HDFC Bank" or "Bank A/c" as merchants
    override val merchantRegex = "(?i)(?:To|Paid to|VPA|at|towards|INFO)[:*]?\\s+(?!HDFC Bank|Bank A/c)(.+?)(?=\\s+\\bOn\\b\\s+\\d|\\s+\\bRef\\b|\\s+\\bRefNo\\b|\\.|$)".toRegex()
    
    // Pattern 2: Specific fallback for UPI and "from VPA" formats
    // Added "from" to handle credit SMS where merchant is at the end
    override val secondaryMerchantRegex = "(?i)(?:To|VPA|from)\\s+([^\\s]+?)(?=\\s+\\bOn\\b\\s+\\d|\\s+\\bRef\\b|\\s+\\bRefNo\\b|\\.|$)".toRegex()
}
