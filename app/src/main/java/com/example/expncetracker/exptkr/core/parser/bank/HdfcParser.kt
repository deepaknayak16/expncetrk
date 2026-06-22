package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex: Regex = Regex(
        "(?:debited|credited|spent|paid|received|withdrawn|Sent)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|payment|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added)".toRegex(RegexOption.IGNORE_CASE)
    
    // Pattern 1: Capture names following common action keywords
    override val merchantRegex = "(?i)(?:To|Paid to|VPA|at|towards|INFO[:*])\\s+(.+?)(?:\\s+On|\\s+Ref|\\s+RefNo|\\.|$)".toRegex()
    
    // Pattern 2: Specific fallback for UPI and "To" formats
    override val secondaryMerchantRegex = "(?i)To\\s+(.+?)(?:\\s|\\.|$)".toRegex()
}
