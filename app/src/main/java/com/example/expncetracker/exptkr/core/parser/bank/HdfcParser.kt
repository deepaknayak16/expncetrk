package com.example.expncetracker.exptkr.core.parser.bank

class HdfcParser : BaseBankParser("HDFC") {
    override val amountRegex: Regex = Regex(
        "(?i)(?:(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned)\\s*.*?(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)|(?:Rs|INR|₹|Amt)\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*.*?(debited|credited|spent|paid|received|withdrawn|Sent|deposited|added|refunded|refund|reversed|reversal|cashback|returned))",
        RegexOption.IGNORE_CASE)
    override val debitRegex = "(?:debited|spent|withdrawn|transferred|paid|sent)".toRegex(RegexOption.IGNORE_CASE)
    override val creditRegex = "(?:credited|deposited|received|added|refunded|refund|reversed|reversal|cashback|returned)".toRegex(RegexOption.IGNORE_CASE)
    
    // Pattern 1: Capture names following common action keywords
    // Priority given to "To" field for UPI/P2P transfers
    override val merchantRegex = "(?i)(?:To|Paid to|VPA|at|towards|INFO)[:*]?\\s+(.+?)(?:\\s+On|\\s+Ref|\\s+RefNo|\\s+at|\\s+towards|\\.|$)".toRegex()
    
    // Pattern 2: Specific fallback for UPI and "To" formats
    // Updated to ensure it captures the full name before common sentinels
    override val secondaryMerchantRegex = "(?i)To[:*]?\\s+(.+?)(?=\\s+On|\\s+Ref|\\s+RefNo|\\.|$)".toRegex()
}
