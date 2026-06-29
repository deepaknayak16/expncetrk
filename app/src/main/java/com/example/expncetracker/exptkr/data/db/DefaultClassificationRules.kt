package com.example.expncetracker.exptkr.data.db

data class DefaultRule(
    val keyword: String,
    val category: String,
    val matchType: String = "CONTAINS",
    val priority: Int = 0,
    val transactionType: String? = null
)

data class DefaultCategory(
    val name: String,
    val type: String, // "INCOME", "EXPENSE"
    val iconName: String,
    val color: Long
)

object DefaultClassificationRules {
    val categories = listOf(
        // Expenses
        DefaultCategory("Food", "EXPENSE", "FOOD", 0xFFF44336),
        DefaultCategory("Groceries", "EXPENSE", "GROCERIES", 0xFF4CAF50),
        DefaultCategory("Cabs", "EXPENSE", "CABS", 0xFF2196F3),
        DefaultCategory("Rent", "EXPENSE", "RENT", 0xFFFF9800),
        DefaultCategory("Bills", "EXPENSE", "BILLS", 0xFF9C27B0),
        DefaultCategory("Shopping", "EXPENSE", "SHOPPING", 0xFFE91E63),
        DefaultCategory("Travel", "EXPENSE", "TRAVEL", 0xFF795548),
        DefaultCategory("Fuel", "EXPENSE", "TRAVEL", 0xFF607D8B),
        DefaultCategory("Healthcare", "EXPENSE", "HEALTHCARE", 0xFFFF5722),
        DefaultCategory("Education", "EXPENSE", "EDUCATION", 0xFF607D8B),
        DefaultCategory("Entertainment", "EXPENSE", "ENTERTAINMENT", 0xFF9C27B0),
        DefaultCategory("Investments", "EXPENSE", "INVESTMENTS", 0xFF00BCD4),
        DefaultCategory("Insurance", "EXPENSE", "HEALTHCARE", 0xFF4CAF50),
        DefaultCategory("Loan & EMI", "EXPENSE", "RENT", 0xFFF44336),
        DefaultCategory("Subscriptions", "EXPENSE", "ENTERTAINMENT", 0xFF2196F3),
        DefaultCategory("Bank Charges", "EXPENSE", "BILLS", 0xFF607D8B),
        DefaultCategory("Taxes", "EXPENSE", "BILLS", 0xFF795548),
        DefaultCategory("Donations", "EXPENSE", "HEALTHCARE", 0xFFE91E63),
        DefaultCategory("Others", "EXPENSE", "OTHERS", 0xFF9E9E9E),

        // Income
        DefaultCategory("Salary", "INCOME", "SALARY", 0xFF4CAF50),
        DefaultCategory("Interest", "INCOME", "SALARY", 0xFF2196F3),
        DefaultCategory("Cashback", "INCOME", "SHOPPING", 0xFFE91E63),
        DefaultCategory("Refund", "INCOME", "BILLS", 0xFF9E9E9E),
        DefaultCategory("Business Income", "INCOME", "SALARY", 0xFFFF9800),
        DefaultCategory("Freelance", "INCOME", "EDUCATION", 0xFF00BCD4),
        DefaultCategory("Dividend", "INCOME", "INVESTMENTS", 0xFF4CAF50),
        DefaultCategory("Rental Income", "INCOME", "RENT", 0xFF795548),
        DefaultCategory("Bonus", "INCOME", "SALARY", 0xFFFF5722),
        DefaultCategory("Gift Received", "INCOME", "SHOPPING", 0xFFEC4899),
        DefaultCategory("Transfer In", "INCOME", "OTHERS", 0xFF64748B),

        // Internal/Transfers
        DefaultCategory("Transfer", "EXPENSE", "OTHERS", 0xFF64748B),
        DefaultCategory("Bank Transfer", "EXPENSE", "OTHERS", 0xFF64748B),
        DefaultCategory("UPI Transfer", "EXPENSE", "OTHERS", 0xFF64748B),
        DefaultCategory("Wallet Transfer", "EXPENSE", "OTHERS", 0xFF64748B),
        DefaultCategory("Cash Withdrawal", "EXPENSE", "OTHERS", 0xFF64748B),
        DefaultCategory("Cash Deposit", "INCOME", "OTHERS", 0xFF64748B),
        DefaultCategory("Credit Card Payment", "EXPENSE", "OTHERS", 0xFF64748B)
    )

    val rules = listOf(

        // ================================
        // INCOME
        // ================================
        DefaultRule("SALARY", "Salary", "CONTAINS", 100, "CREDIT"),
        DefaultRule("PAYROLL", "Salary", "CONTAINS", 100, "CREDIT"),
        DefaultRule("SAL CREDIT", "Salary", "CONTAINS", 100, "CREDIT"),
        DefaultRule("SALARY CREDIT", "Salary", "CONTAINS", 100, "CREDIT"),
        DefaultRule("MONTHLY SALARY", "Salary", "CONTAINS", 100, "CREDIT"),

        DefaultRule("TCS", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("INFOSYS", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("WIPRO", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("HCL", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("COGNIZANT", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("ACCENTURE", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("IBM", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("CAPGEMINI", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("LTIM", "Salary", "CONTAINS", 90, "CREDIT"),
        DefaultRule("TECH MAHINDRA", "Salary", "CONTAINS", 90, "CREDIT"),

        DefaultRule("CASHBACK", "Cashback", "CONTAINS", 80, "CREDIT"),
        DefaultRule("REWARD", "Cashback", "CONTAINS", 80, "CREDIT"),
        DefaultRule("REFUND", "Refund", "CONTAINS", 80, "CREDIT"),
        DefaultRule("CREDITED", "Salary", "CONTAINS", 80, "CREDIT"),

        // ================================
        // INCOME - Business Income
        // ================================
        DefaultRule("BUSINESS", "Business Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("INVOICE", "Business Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("CLIENT PAYMENT", "Business Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("CONSULTING", "Business Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("SERVICE PAYMENT", "Business Income", "CONTAINS", 90, "CREDIT"),

        // ================================
        // INCOME - Freelance
        // ================================
        DefaultRule("FREELANCE", "Freelance", "CONTAINS", 90, "CREDIT"),
        DefaultRule("UPWORK", "Freelance", "CONTAINS", 90, "CREDIT"),
        DefaultRule("FIVERR", "Freelance", "CONTAINS", 90, "CREDIT"),
        DefaultRule("TOPTAL", "Freelance", "CONTAINS", 90, "CREDIT"),
        DefaultRule("PAYONEER", "Freelance", "CONTAINS", 90, "CREDIT"),

        // ================================
        // INCOME - Interest
        // ================================
        DefaultRule("INTEREST", "Interest", "CONTAINS", 90, "CREDIT"),
        DefaultRule("FD INTEREST", "Interest", "CONTAINS", 90, "CREDIT"),
        DefaultRule("SB INTEREST", "Interest", "CONTAINS", 90, "CREDIT"),
        DefaultRule("SAVINGS INTEREST", "Interest", "CONTAINS", 90, "CREDIT"),

        // ================================
        // INCOME - Dividend
        // ================================
        DefaultRule("DIVIDEND", "Dividend", "CONTAINS", 90, "CREDIT"),
        DefaultRule("DIV PAY", "Dividend", "CONTAINS", 90, "CREDIT"),

        // ================================
        // INCOME - Rental Income
        // ================================
        DefaultRule("HOUSE RENT RECEIVED", "Rental Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("RENT RECEIVED", "Rental Income", "CONTAINS", 90, "CREDIT"),
        DefaultRule("RENTAL", "Rental Income", "CONTAINS", 90, "CREDIT"),

        // ================================
        // INCOME - Bonus
        // ================================
        DefaultRule("BONUS", "Bonus", "CONTAINS", 95, "CREDIT"),
        DefaultRule("INCENTIVE", "Bonus", "CONTAINS", 95, "CREDIT"),
        DefaultRule("PERFORMANCE BONUS", "Bonus", "CONTAINS", 95, "CREDIT"),

        // ================================
        // INCOME - Gift Received
        // ================================
        DefaultRule("GIFT", "Gift Received", "CONTAINS", 80, "CREDIT"),
        DefaultRule("GIFT RECEIVED", "Gift Received", "CONTAINS", 80, "CREDIT"),
        DefaultRule("MONETARY GIFT", "Gift Received", "CONTAINS", 80, "CREDIT"),

        // ================================
        // INCOME - Transfer In
        // ================================
        DefaultRule("TRANSFER FROM", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("BANK TRANSFER", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("NEFT CR", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("IMPS CR", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("RTGS CR", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("UPI CR", "Transfer In", "CONTAINS", 100, "CREDIT"),
        DefaultRule("ACCOUNT TRANSFER", "Transfer In", "CONTAINS", 100, "CREDIT"),

        // ================================
        // FOOD
        // ================================
        DefaultRule("SWIGGY", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("ZOMATO", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("EATCLUB", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BOX8", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("FAASOS", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BEHROUZ", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("FRESHMENU", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("DOMINOS", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("PIZZA HUT", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("KFC", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("MCDONALD", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BURGER KING", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("SUBWAY", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("STARBUCKS", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("CCD", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("CAFE COFFEE DAY", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("THIRD WAVE", "Food", "CONTAINS", 90, "DEBIT"),
        DefaultRule("HOTEL", "Food", "CONTAINS", 85, "DEBIT"),
        DefaultRule("RESTAURANT", "Food", "CONTAINS", 85, "DEBIT"),
        DefaultRule("CAFE", "Food", "CONTAINS", 85, "DEBIT"),
        DefaultRule("BAKERY", "Food", "CONTAINS", 85, "DEBIT"),
        DefaultRule("SWEETS", "Food", "CONTAINS", 85, "DEBIT"),

        // ================================
        // GROCERIES
        // ================================
        DefaultRule("BIGBASKET", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BBNOW", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BLINKIT", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("ZEPTO", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("INSTAMART", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("DMART", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("RELIANCE FRESH", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("SPAR", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("MORE", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("NATURES BASKET", "Groceries", "CONTAINS", 90, "DEBIT"),
        DefaultRule("SUPER MARKET", "Groceries", "CONTAINS", 85, "DEBIT"),
        DefaultRule("HYPERMARKET", "Groceries", "CONTAINS", 85, "DEBIT"),
        DefaultRule("PROVISION", "Groceries", "CONTAINS", 85, "DEBIT"),
        DefaultRule("SUPERMARKET", "Groceries", "CONTAINS", 85, "DEBIT"),
        DefaultRule("KIRANA", "Groceries", "CONTAINS", 85, "DEBIT"),

        // ================================
        // SHOPPING
        // ================================
        DefaultRule("AMAZON", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("FLIPKART", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("MYNTRA", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("AJIO", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("NYKAA", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("MEESHO", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("TATACLIQ", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("CROMA", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("RELIANCE DIGITAL", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("VIJAY SALES", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("DECATHLON", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("IKEA", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("FIRSTCRY", "Shopping", "CONTAINS", 90, "DEBIT"),
        DefaultRule("FANCY", "Shopping", "CONTAINS", 80, "DEBIT"),
        DefaultRule("TOYS", "Shopping", "CONTAINS", 80, "DEBIT"),

        // ================================
        // TRANSPORT
        // ================================
        DefaultRule("UBER", "Cabs", "CONTAINS", 95, "DEBIT"),
        DefaultRule("OLA", "Cabs", "CONTAINS", 95, "DEBIT"),
        DefaultRule("RAPIDO", "Cabs", "CONTAINS", 95, "DEBIT"),
        DefaultRule("NAMMA METRO", "Cabs", "CONTAINS", 95, "DEBIT"),
        DefaultRule("BMTC", "Cabs", "CONTAINS", 95, "DEBIT"),

        // ================================
        // TRAVEL
        // ================================
        DefaultRule("IRCTC", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("REDBUS", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ABHIBUS", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MAKEMYTRIP", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("GOIBIBO", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("YATRA", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("AIR INDIA", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("INDIGO", "Travel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("AKASA", "Travel", "CONTAINS", 0, "DEBIT"),

        // ================================
        // FUEL
        // ================================
        DefaultRule("BPCL", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("HPCL", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("IOC", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("INDIAN OIL", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("BHARAT PETROLEUM", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SHELL", "Fuel", "CONTAINS", 0, "DEBIT"),
        DefaultRule("NAYARA", "Fuel", "CONTAINS", 0, "DEBIT"),

        // ================================
        // HEALTHCARE
        // ================================
        DefaultRule("APOLLO PHARMACY", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("APOLLO HOSPITAL", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MEDPLUS", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("1MG", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PHARMEASY", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("FORTIS", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MANIPAL", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("NARAYANA", "Healthcare", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MAX HOSPITAL", "Healthcare", "CONTAINS", 0, "DEBIT"),

        // ================================
        // EDUCATION
        // ================================
        DefaultRule("UNACADEMY", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("BYJUS", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PW", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PHYSICS WALLAH", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("UDEMY", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("COURSERA", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("UPGRAD", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SCHOOL", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("COLLEGE", "Education", "CONTAINS", 0, "DEBIT"),
        DefaultRule("UNIVERSITY", "Education", "CONTAINS", 0, "DEBIT"),

        // ================================
        // BILLS
        // ================================
        DefaultRule("BESCOM", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BWSSB", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("AIRTEL", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("JIO", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("VI", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("BSNL", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("ACT FIBERNET", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("HATHWAY", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("TATAPLAY", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("VI POSTPAID", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("VI PREPAID", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("VODAFONE IDEA", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("DRINKPRIME", "Bills", "CONTAINS", 90, "DEBIT"),
        DefaultRule("CONVERGENCE", "Bills", "CONTAINS", 85, "DEBIT"),
        DefaultRule("POSTPAID", "Bills", "CONTAINS", 85, "DEBIT"),
        
        // FIX BUG-ML-08: EPFO should be Investments, not Salary income.
        DefaultRule("EPFO", "Investments", "CONTAINS", 90, "DEBIT"),

        // Bank / Transfer Fallbacks (Prevent Groceries trap)
        DefaultRule("HDFC BANK", "Transfer", "CONTAINS", 70),
        DefaultRule("KOTAK BANK", "Transfer", "CONTAINS", 70),
        DefaultRule("AXIS BANK", "Transfer", "CONTAINS", 70),
        DefaultRule("ICICI BANK", "Transfer", "CONTAINS", 70),
        DefaultRule("SBI", "Transfer", "CONTAINS", 70),
        DefaultRule("BANK AC", "Transfer", "CONTAINS", 70),
        DefaultRule("CREDITED TO", "Transfer", "CONTAINS", 70),
        DefaultRule("DEBITED FROM", "Transfer", "CONTAINS", 70),
        DefaultRule("TRANSACTION", "Others", "CONTAINS", 60),
        DefaultRule("@", "Transfer", "CONTAINS", 70, "DEBIT"),
        DefaultRule("PAYTM", "Transfer", "CONTAINS", 80, "DEBIT"),
        DefaultRule("PHONEPE", "Transfer", "CONTAINS", 80, "DEBIT"),

        // ================================
        // ENTERTAINMENT
        // ================================
        DefaultRule("NETFLIX", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SPOTIFY", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PRIME VIDEO", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("HOTSTAR", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("JIOHOTSTAR", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SONY LIV", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ZEE5", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("BOOKMYSHOW", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PVR", "Entertainment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("INOX", "Entertainment", "CONTAINS", 0, "DEBIT"),

        // ================================
        // INVESTMENTS
        // ================================
        DefaultRule("ZERODHA", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("GROWW", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("UPSTOX", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ANGEL ONE", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PAYTM MONEY", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SMALLCASE", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("KUVERA", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MUTUAL FUND", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SIP", "Investments", "CONTAINS", 0, "DEBIT"),
        DefaultRule("NPS", "Investments", "CONTAINS", 0, "DEBIT"),

        // ================================
        // INSURANCE
        // ================================
        DefaultRule("LIC", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ACKO", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("STAR HEALTH", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("HDFC ERGO", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ICICI LOMBARD", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("NIVA BUPA", "Insurance", "CONTAINS", 0, "DEBIT"),
        DefaultRule("BAJAJ ALLIANZ", "Insurance", "CONTAINS", 0, "DEBIT"),

        // ================================
        // LOANS & EMI
        // ================================
        DefaultRule("EMI", "Loan & EMI", "CONTAINS", 0, "DEBIT"),
        DefaultRule("HOME LOAN", "Loan & EMI", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PERSONAL LOAN", "Loan & EMI", "CONTAINS", 0, "DEBIT"),
        DefaultRule("VEHICLE LOAN", "Loan & EMI", "CONTAINS", 0, "DEBIT"),

        // ================================
        // RENT
        // ================================
        DefaultRule("HOUSE RENT", "Rent", "CONTAINS", 0, "DEBIT"),
        DefaultRule("RENT", "Rent", "CONTAINS", 0, "DEBIT"),
        DefaultRule("HOSTEL", "Rent", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PG", "Rent", "CONTAINS", 0, "DEBIT"),

        // ================================
        // SUBSCRIPTIONS
        // ================================
        DefaultRule("GOOGLE ONE", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("APPLE", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ICLOUD", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MICROSOFT", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ADOBE", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CHATGPT", "Subscriptions", "CONTAINS", 0, "DEBIT"),
        DefaultRule("YOUTUBE PREMIUM", "Subscriptions", "CONTAINS", 0, "DEBIT"),

        // ================================
        // BANKING
        // ================================
        DefaultRule("ATM", "Bank Charges", "CONTAINS", 0, "DEBIT"),
        
        // FIX BUG-ML-09: Removed duplicate ATM WDL from Bank Charges (should be Cash Withdrawal)
        // DefaultRule("ATM WDL", "Bank Charges", "CONTAINS"), 
        
        DefaultRule("CASH WITHDRAWAL", "Bank Charges", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SERVICE CHARGE", "Bank Charges", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ANNUAL FEE", "Bank Charges", "CONTAINS", 0, "DEBIT"),
        DefaultRule("LATE FEE", "Bank Charges", "CONTAINS", 0, "DEBIT"),

        // ================================
        // TAX
        // ================================
        DefaultRule("INCOME TAX", "Taxes", "CONTAINS", 0, "DEBIT"),
        DefaultRule("GST", "Taxes", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PROPERTY TAX", "Taxes", "CONTAINS", 0, "DEBIT"),

        // ================================
        // DONATION
        // ================================
        DefaultRule("DONATION", "Donations", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CHARITY", "Donations", "CONTAINS", 0, "DEBIT"),
        DefaultRule("TEMPLE", "Donations", "CONTAINS", 0, "DEBIT"),
        DefaultRule("TRUST", "Donations", "CONTAINS", 0, "DEBIT"),

        // ================================
        // TRANSFERS (Ignored in reports)
        // ================================
        DefaultRule("TRANSFER", "Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("SELF TRANSFER", "Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ACCOUNT TRANSFER", "Transfer", "CONTAINS", 0, "DEBIT"),

        DefaultRule("BANK TRANSFER", "Bank Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("NEFT", "Bank Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("RTGS", "Bank Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("IMPS", "Bank Transfer", "CONTAINS", 0, "DEBIT"),

        // FIX BUG-ML-10: UPI rule should be EXACT to avoid matching "AXIS-UPI" etc incorrectly
        DefaultRule("UPI", "UPI Transfer", "EXACT", 50, "DEBIT"),

        DefaultRule("UPI PAYMENT", "UPI Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("UPI TRANSFER", "UPI Transfer", "CONTAINS", 0, "DEBIT"),

        DefaultRule("PAYTM WALLET", "Wallet Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("PHONEPE WALLET", "Wallet Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("AMAZON PAY", "Wallet Transfer", "CONTAINS", 0, "DEBIT"),
        DefaultRule("MOBIKWIK", "Wallet Transfer", "CONTAINS", 0, "DEBIT"),

        DefaultRule("ATM WDL", "Cash Withdrawal", "CONTAINS", 0, "DEBIT"),
        DefaultRule("ATM WITHDRAWAL", "Cash Withdrawal", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CASH WDL", "Cash Withdrawal", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CASH WITHDRAWAL", "Cash Withdrawal", "CONTAINS", 0, "DEBIT"),

        DefaultRule("CASH DEPOSIT", "Cash Deposit", "CONTAINS"),
        DefaultRule("CDM", "Cash Deposit", "CONTAINS"),
        DefaultRule("BRANCH CASH", "Cash Deposit", "CONTAINS"),

        DefaultRule("CREDIT CARD PAYMENT", "Credit Card Payment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CC PAYMENT", "Credit Card Payment", "CONTAINS", 0, "DEBIT"),
        DefaultRule("CARD PAYMENT", "Credit Card Payment", "CONTAINS", 0, "DEBIT")
    )
}
