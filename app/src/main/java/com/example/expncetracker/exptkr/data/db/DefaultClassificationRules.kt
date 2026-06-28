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
        DefaultRule("SWIGGY", "Food", "CONTAINS", 90),
        DefaultRule("ZOMATO", "Food", "CONTAINS", 90),
        DefaultRule("EATCLUB", "Food", "CONTAINS", 90),
        DefaultRule("BOX8", "Food", "CONTAINS", 90),
        DefaultRule("FAASOS", "Food", "CONTAINS", 90),
        DefaultRule("BEHROUZ", "Food", "CONTAINS", 90),
        DefaultRule("FRESHMENU", "Food", "CONTAINS", 90),
        DefaultRule("DOMINOS", "Food", "CONTAINS", 90),
        DefaultRule("PIZZA HUT", "Food", "CONTAINS", 90),
        DefaultRule("KFC", "Food", "CONTAINS", 90),
        DefaultRule("MCDONALD", "Food", "CONTAINS", 90),
        DefaultRule("BURGER KING", "Food", "CONTAINS", 90),
        DefaultRule("SUBWAY", "Food", "CONTAINS", 90),
        DefaultRule("STARBUCKS", "Food", "CONTAINS", 90),
        DefaultRule("CCD", "Food", "CONTAINS", 90),
        DefaultRule("CAFE COFFEE DAY", "Food", "CONTAINS", 90),
        DefaultRule("THIRD WAVE", "Food", "CONTAINS", 90),
        DefaultRule("HOTEL", "Food", "CONTAINS", 85),
        DefaultRule("RESTAURANT", "Food", "CONTAINS", 85),
        DefaultRule("CAFE", "Food", "CONTAINS", 85),
        DefaultRule("BAKERY", "Food", "CONTAINS", 85),

        // ================================
        // GROCERIES
        // ================================
        DefaultRule("BIGBASKET", "Groceries", "CONTAINS", 90),
        DefaultRule("BBNOW", "Groceries", "CONTAINS", 90),
        DefaultRule("BLINKIT", "Groceries", "CONTAINS", 90),
        DefaultRule("ZEPTO", "Groceries", "CONTAINS", 90),
        DefaultRule("INSTAMART", "Groceries", "CONTAINS", 90),
        DefaultRule("DMART", "Groceries", "CONTAINS", 90),
        DefaultRule("RELIANCE FRESH", "Groceries", "CONTAINS", 90),
        DefaultRule("SPAR", "Groceries", "CONTAINS", 90),
        DefaultRule("MORE", "Groceries", "CONTAINS", 90),
        DefaultRule("NATURES BASKET", "Groceries", "CONTAINS", 90),
        DefaultRule("SUPER MARKET", "Groceries", "CONTAINS", 85),
        DefaultRule("HYPERMARKET", "Groceries", "CONTAINS", 85),
        DefaultRule("PROVISION", "Groceries", "CONTAINS", 85),

        // ================================
        // SHOPPING
        // ================================
        DefaultRule("AMAZON", "Shopping", "CONTAINS", 90),
        DefaultRule("FLIPKART", "Shopping", "CONTAINS", 90),
        DefaultRule("MYNTRA", "Shopping", "CONTAINS", 90),
        DefaultRule("AJIO", "Shopping", "CONTAINS", 90),
        DefaultRule("NYKAA", "Shopping", "CONTAINS", 90),
        DefaultRule("MEESHO", "Shopping", "CONTAINS", 90),
        DefaultRule("TATACLIQ", "Shopping", "CONTAINS", 90),
        DefaultRule("CROMA", "Shopping", "CONTAINS", 90),
        DefaultRule("RELIANCE DIGITAL", "Shopping", "CONTAINS", 90),
        DefaultRule("VIJAY SALES", "Shopping", "CONTAINS", 90),
        DefaultRule("DECATHLON", "Shopping", "CONTAINS", 90),
        DefaultRule("IKEA", "Shopping", "CONTAINS", 90),
        DefaultRule("FIRSTCRY", "Shopping", "CONTAINS", 90),

        // ================================
        // TRANSPORT
        // ================================
        DefaultRule("UBER", "Cabs", "CONTAINS", 95),
        DefaultRule("OLA", "Cabs", "CONTAINS", 95),
        DefaultRule("RAPIDO", "Cabs", "CONTAINS", 95),
        DefaultRule("NAMMA METRO", "Cabs", "CONTAINS", 95),
        DefaultRule("BMTC", "Cabs", "CONTAINS", 95),

        // ================================
        // TRAVEL
        // ================================
        DefaultRule("IRCTC", "Travel", "CONTAINS"),
        DefaultRule("REDBUS", "Travel", "CONTAINS"),
        DefaultRule("ABHIBUS", "Travel", "CONTAINS"),
        DefaultRule("MAKEMYTRIP", "Travel", "CONTAINS"),
        DefaultRule("GOIBIBO", "Travel", "CONTAINS"),
        DefaultRule("YATRA", "Travel", "CONTAINS"),
        DefaultRule("AIR INDIA", "Travel", "CONTAINS"),
        DefaultRule("INDIGO", "Travel", "CONTAINS"),
        DefaultRule("AKASA", "Travel", "CONTAINS"),

        // ================================
        // FUEL
        // ================================
        DefaultRule("BPCL", "Fuel", "CONTAINS"),
        DefaultRule("HPCL", "Fuel", "CONTAINS"),
        DefaultRule("IOC", "Fuel", "CONTAINS"),
        DefaultRule("INDIAN OIL", "Fuel", "CONTAINS"),
        DefaultRule("BHARAT PETROLEUM", "Fuel", "CONTAINS"),
        DefaultRule("SHELL", "Fuel", "CONTAINS"),
        DefaultRule("NAYARA", "Fuel", "CONTAINS"),

        // ================================
        // HEALTHCARE
        // ================================
        DefaultRule("APOLLO PHARMACY", "Healthcare", "CONTAINS"),
        DefaultRule("APOLLO HOSPITAL", "Healthcare", "CONTAINS"),
        DefaultRule("MEDPLUS", "Healthcare", "CONTAINS"),
        DefaultRule("1MG", "Healthcare", "CONTAINS"),
        DefaultRule("PHARMEASY", "Healthcare", "CONTAINS"),
        DefaultRule("FORTIS", "Healthcare", "CONTAINS"),
        DefaultRule("MANIPAL", "Healthcare", "CONTAINS"),
        DefaultRule("NARAYANA", "Healthcare", "CONTAINS"),
        DefaultRule("MAX HOSPITAL", "Healthcare", "CONTAINS"),

        // ================================
        // EDUCATION
        // ================================
        DefaultRule("UNACADEMY", "Education", "CONTAINS"),
        DefaultRule("BYJUS", "Education", "CONTAINS"),
        DefaultRule("PW", "Education", "CONTAINS"),
        DefaultRule("PHYSICS WALLAH", "Education", "CONTAINS"),
        DefaultRule("UDEMY", "Education", "CONTAINS"),
        DefaultRule("COURSERA", "Education", "CONTAINS"),
        DefaultRule("UPGRAD", "Education", "CONTAINS"),
        DefaultRule("SCHOOL", "Education", "CONTAINS"),
        DefaultRule("COLLEGE", "Education", "CONTAINS"),
        DefaultRule("UNIVERSITY", "Education", "CONTAINS"),

        // ================================
        // BILLS
        // ================================
        DefaultRule("BESCOM", "Bills", "CONTAINS", 90),
        DefaultRule("BWSSB", "Bills", "CONTAINS", 90),
        DefaultRule("AIRTEL", "Bills", "CONTAINS", 90),
        DefaultRule("JIO", "Bills", "CONTAINS", 90),
        DefaultRule("VI", "Bills", "CONTAINS", 90),
        DefaultRule("BSNL", "Bills", "CONTAINS", 90),
        DefaultRule("ACT FIBERNET", "Bills", "CONTAINS", 90),
        DefaultRule("HATHWAY", "Bills", "CONTAINS", 90),
        DefaultRule("TATAPLAY", "Bills", "CONTAINS", 90),
        DefaultRule("VI POSTPAID", "Bills", "CONTAINS", 90),
        DefaultRule("VI PREPAID", "Bills", "CONTAINS", 90),
        DefaultRule("VODAFONE IDEA", "Bills", "CONTAINS", 90),
        DefaultRule("EPFO", "Salary", "CONTAINS", 100, "CREDIT"),

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

        // ================================
        // ENTERTAINMENT
        // ================================
        DefaultRule("NETFLIX", "Entertainment", "CONTAINS"),
        DefaultRule("SPOTIFY", "Entertainment", "CONTAINS"),
        DefaultRule("PRIME VIDEO", "Entertainment", "CONTAINS"),
        DefaultRule("HOTSTAR", "Entertainment", "CONTAINS"),
        DefaultRule("JIOHOTSTAR", "Entertainment", "CONTAINS"),
        DefaultRule("SONY LIV", "Entertainment", "CONTAINS"),
        DefaultRule("ZEE5", "Entertainment", "CONTAINS"),
        DefaultRule("BOOKMYSHOW", "Entertainment", "CONTAINS"),
        DefaultRule("PVR", "Entertainment", "CONTAINS"),
        DefaultRule("INOX", "Entertainment", "CONTAINS"),

        // ================================
        // INVESTMENTS
        // ================================
        DefaultRule("ZERODHA", "Investments", "CONTAINS"),
        DefaultRule("GROWW", "Investments", "CONTAINS"),
        DefaultRule("UPSTOX", "Investments", "CONTAINS"),
        DefaultRule("ANGEL ONE", "Investments", "CONTAINS"),
        DefaultRule("PAYTM MONEY", "Investments", "CONTAINS"),
        DefaultRule("SMALLCASE", "Investments", "CONTAINS"),
        DefaultRule("KUVERA", "Investments", "CONTAINS"),
        DefaultRule("MUTUAL FUND", "Investments", "CONTAINS"),
        DefaultRule("SIP", "Investments", "CONTAINS"),
        DefaultRule("NPS", "Investments", "CONTAINS"),

        // ================================
        // INSURANCE
        // ================================
        DefaultRule("LIC", "Insurance", "CONTAINS"),
        DefaultRule("ACKO", "Insurance", "CONTAINS"),
        DefaultRule("STAR HEALTH", "Insurance", "CONTAINS"),
        DefaultRule("HDFC ERGO", "Insurance", "CONTAINS"),
        DefaultRule("ICICI LOMBARD", "Insurance", "CONTAINS"),
        DefaultRule("NIVA BUPA", "Insurance", "CONTAINS"),
        DefaultRule("BAJAJ ALLIANZ", "Insurance", "CONTAINS"),

        // ================================
        // LOANS & EMI
        // ================================
        DefaultRule("EMI", "Loan & EMI", "CONTAINS"),
        DefaultRule("HOME LOAN", "Loan & EMI", "CONTAINS"),
        DefaultRule("PERSONAL LOAN", "Loan & EMI", "CONTAINS"),
        DefaultRule("VEHICLE LOAN", "Loan & EMI", "CONTAINS"),

        // ================================
        // RENT
        // ================================
        DefaultRule("HOUSE RENT", "Rent", "CONTAINS"),
        DefaultRule("RENT", "Rent", "CONTAINS"),
        DefaultRule("HOSTEL", "Rent", "CONTAINS"),
        DefaultRule("PG", "Rent", "CONTAINS"),

        // ================================
        // SUBSCRIPTIONS
        // ================================
        DefaultRule("GOOGLE ONE", "Subscriptions", "CONTAINS"),
        DefaultRule("APPLE", "Subscriptions", "CONTAINS"),
        DefaultRule("ICLOUD", "Subscriptions", "CONTAINS"),
        DefaultRule("MICROSOFT", "Subscriptions", "CONTAINS"),
        DefaultRule("ADOBE", "Subscriptions", "CONTAINS"),
        DefaultRule("CHATGPT", "Subscriptions", "CONTAINS"),
        DefaultRule("YOUTUBE PREMIUM", "Subscriptions", "CONTAINS"),

        // ================================
        // BANKING
        // ================================
        DefaultRule("ATM", "Bank Charges", "CONTAINS"),
        DefaultRule("ATM WDL", "Bank Charges", "CONTAINS"),
        DefaultRule("CASH WITHDRAWAL", "Bank Charges", "CONTAINS"),
        DefaultRule("SERVICE CHARGE", "Bank Charges", "CONTAINS"),
        DefaultRule("ANNUAL FEE", "Bank Charges", "CONTAINS"),
        DefaultRule("LATE FEE", "Bank Charges", "CONTAINS"),

        // ================================
        // TAX
        // ================================
        DefaultRule("INCOME TAX", "Taxes", "CONTAINS"),
        DefaultRule("GST", "Taxes", "CONTAINS"),
        DefaultRule("PROPERTY TAX", "Taxes", "CONTAINS"),

        // ================================
        // DONATION
        // ================================
        DefaultRule("DONATION", "Donations", "CONTAINS"),
        DefaultRule("CHARITY", "Donations", "CONTAINS"),
        DefaultRule("TEMPLE", "Donations", "CONTAINS"),
        DefaultRule("TRUST", "Donations", "CONTAINS"),

        // ================================
        // TRANSFERS (Ignored in reports)
        // ================================
        DefaultRule("TRANSFER", "Transfer", "CONTAINS"),
        DefaultRule("SELF TRANSFER", "Transfer", "CONTAINS"),
        DefaultRule("ACCOUNT TRANSFER", "Transfer", "CONTAINS"),

        DefaultRule("BANK TRANSFER", "Bank Transfer", "CONTAINS"),
        DefaultRule("NEFT", "Bank Transfer", "CONTAINS"),
        DefaultRule("RTGS", "Bank Transfer", "CONTAINS"),
        DefaultRule("IMPS", "Bank Transfer", "CONTAINS"),

        DefaultRule("UPI", "UPI Transfer", "CONTAINS"),
        DefaultRule("UPI PAYMENT", "UPI Transfer", "CONTAINS"),
        DefaultRule("UPI TRANSFER", "UPI Transfer", "CONTAINS"),

        DefaultRule("PAYTM WALLET", "Wallet Transfer", "CONTAINS"),
        DefaultRule("PHONEPE WALLET", "Wallet Transfer", "CONTAINS"),
        DefaultRule("AMAZON PAY", "Wallet Transfer", "CONTAINS"),
        DefaultRule("MOBIKWIK", "Wallet Transfer", "CONTAINS"),

        DefaultRule("ATM WDL", "Cash Withdrawal", "CONTAINS"),
        DefaultRule("ATM WITHDRAWAL", "Cash Withdrawal", "CONTAINS"),
        DefaultRule("CASH WDL", "Cash Withdrawal", "CONTAINS"),
        DefaultRule("CASH WITHDRAWAL", "Cash Withdrawal", "CONTAINS"),

        DefaultRule("CASH DEPOSIT", "Cash Deposit", "CONTAINS"),
        DefaultRule("CDM", "Cash Deposit", "CONTAINS"),
        DefaultRule("BRANCH CASH", "Cash Deposit", "CONTAINS"),

        DefaultRule("CREDIT CARD PAYMENT", "Credit Card Payment", "CONTAINS"),
        DefaultRule("CC PAYMENT", "Credit Card Payment", "CONTAINS"),
        DefaultRule("CARD PAYMENT", "Credit Card Payment", "CONTAINS")
    )
}