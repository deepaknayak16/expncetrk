package com.example.expncetracker.exptkr.data.db

data class DefaultRule(
    val pattern: String,
    val category: String,
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
        DefaultRule("SALARY", "Salary", 100, "CREDIT"),
        DefaultRule("PAYROLL", "Salary", 100, "CREDIT"),
        DefaultRule("SAL CREDIT", "Salary", 100, "CREDIT"),
        DefaultRule("SALARY CREDIT", "Salary", 100, "CREDIT"),
        DefaultRule("MONTHLY SALARY", "Salary", 100, "CREDIT"),

        DefaultRule("TCS", "Salary", 90, "CREDIT"),
        DefaultRule("INFOSYS", "Salary", 90, "CREDIT"),
        DefaultRule("WIPRO", "Salary", 90, "CREDIT"),
        DefaultRule("HCL", "Salary", 90, "CREDIT"),
        DefaultRule("COGNIZANT", "Salary", 90, "CREDIT"),
        DefaultRule("ACCENTURE", "Salary", 90, "CREDIT"),
        DefaultRule("IBM", "Salary", 90, "CREDIT"),
        DefaultRule("CAPGEMINI", "Salary", 90, "CREDIT"),
        DefaultRule("LTIM", "Salary", 90, "CREDIT"),
        DefaultRule("TECH MAHINDRA", "Salary", 90, "CREDIT"),

        DefaultRule("INTEREST", "Interest", 80, "CREDIT"),
        DefaultRule("FD INTEREST", "Interest", 80, "CREDIT"),
        DefaultRule("CASHBACK", "Cashback", 80, "CREDIT"),
        DefaultRule("REWARD", "Cashback", 80, "CREDIT"),
        DefaultRule("REFUND", "Refund", 80, "CREDIT"),

        // ================================
        // INCOME - Business Income
        // ================================
        DefaultRule("BUSINESS", "Business Income", 90, "CREDIT"),
        DefaultRule("INVOICE", "Business Income", 90, "CREDIT"),
        DefaultRule("CLIENT PAYMENT", "Business Income", 90, "CREDIT"),
        DefaultRule("CONSULTING", "Business Income", 90, "CREDIT"),
        DefaultRule("SERVICE PAYMENT", "Business Income", 90, "CREDIT"),

        // ================================
        // INCOME - Freelance
        // ================================
        DefaultRule("FREELANCE", "Freelance", 90, "CREDIT"),
        DefaultRule("UPWORK", "Freelance", 90, "CREDIT"),
        DefaultRule("FIVERR", "Freelance", 90, "CREDIT"),
        DefaultRule("TOPTAL", "Freelance", 90, "CREDIT"),
        DefaultRule("PAYONEER", "Freelance", 90, "CREDIT"),

        // ================================
        // INCOME - Interest
        // ================================
        DefaultRule("INTEREST", "Interest", 90, "CREDIT"),
        DefaultRule("FD INTEREST", "Interest", 90, "CREDIT"),
        DefaultRule("SB INTEREST", "Interest", 90, "CREDIT"),
        DefaultRule("SAVINGS INTEREST", "Interest", 90, "CREDIT"),

        // ================================
        // INCOME - Dividend
        // ================================
        DefaultRule("DIVIDEND", "Dividend", 90, "CREDIT"),
        DefaultRule("DIV PAY", "Dividend", 90, "CREDIT"),

        // ================================
        // INCOME - Rental Income
        // ================================
        DefaultRule("HOUSE RENT RECEIVED", "Rental Income", 90, "CREDIT"),
        DefaultRule("RENT RECEIVED", "Rental Income", 90, "CREDIT"),
        DefaultRule("RENTAL", "Rental Income", 90, "CREDIT"),

        // ================================
        // INCOME - Refund
        // ================================
        DefaultRule("REFUND", "Refund", 95, "CREDIT"),
        DefaultRule("REVERSAL", "Refund", 95, "CREDIT"),
        DefaultRule("REVERSAL CREDIT", "Refund", 95, "CREDIT"),
        DefaultRule("FAILED TXN REVERSAL", "Refund", 95, "CREDIT"),

        // ================================
        // INCOME - Cashback
        // ================================
        DefaultRule("CASHBACK", "Cashback", 95, "CREDIT"),
        DefaultRule("REWARD", "Cashback", 95, "CREDIT"),
        DefaultRule("REWARDS", "Cashback", 95, "CREDIT"),
        DefaultRule("BONUS CASH", "Cashback", 95, "CREDIT"),

        // ================================
        // INCOME - Bonus
        // ================================
        DefaultRule("BONUS", "Bonus", 95, "CREDIT"),
        DefaultRule("INCENTIVE", "Bonus", 95, "CREDIT"),
        DefaultRule("PERFORMANCE BONUS", "Bonus", 95, "CREDIT"),

        // ================================
        // INCOME - Gift Received
        // ================================
        DefaultRule("GIFT", "Gift Received", 80, "CREDIT"),
        DefaultRule("GIFT RECEIVED", "Gift Received", 80, "CREDIT"),
        DefaultRule("MONETARY GIFT", "Gift Received", 80, "CREDIT"),

        // ================================
        // INCOME - Transfer In
        // ================================
        DefaultRule("TRANSFER FROM", "Transfer In", 100, "CREDIT"),
        DefaultRule("BANK TRANSFER", "Transfer In", 100, "CREDIT"),
        DefaultRule("NEFT CR", "Transfer In", 100, "CREDIT"),
        DefaultRule("IMPS CR", "Transfer In", 100, "CREDIT"),
        DefaultRule("RTGS CR", "Transfer In", 100, "CREDIT"),
        DefaultRule("UPI CR", "Transfer In", 100, "CREDIT"),
        DefaultRule("ACCOUNT TRANSFER", "Transfer In", 100, "CREDIT"),
        // ================================
        // FOOD
        // ================================
        DefaultRule("SWIGGY", "Food"),
        DefaultRule("ZOMATO", "Food"),
        DefaultRule("EATCLUB", "Food"),
        DefaultRule("BOX8", "Food"),
        DefaultRule("FAASOS", "Food"),
        DefaultRule("BEHROUZ", "Food"),
        DefaultRule("FRESHMENU", "Food"),
        DefaultRule("DOMINOS", "Food"),
        DefaultRule("PIZZA HUT", "Food"),
        DefaultRule("KFC", "Food"),
        DefaultRule("MCDONALD", "Food"),
        DefaultRule("BURGER KING", "Food"),
        DefaultRule("SUBWAY", "Food"),
        DefaultRule("STARBUCKS", "Food"),
        DefaultRule("CCD", "Food"),
        DefaultRule("CAFE COFFEE DAY", "Food"),
        DefaultRule("THIRD WAVE", "Food"),

        // ================================
        // GROCERIES
        // ================================
        DefaultRule("BIGBASKET", "Groceries"),
        DefaultRule("BBNOW", "Groceries"),
        DefaultRule("BLINKIT", "Groceries"),
        DefaultRule("ZEPTO", "Groceries"),
        DefaultRule("INSTAMART", "Groceries"),
        DefaultRule("DMART", "Groceries"),
        DefaultRule("RELIANCE FRESH", "Groceries"),
        DefaultRule("SPAR", "Groceries"),
        DefaultRule("MORE", "Groceries"),
        DefaultRule("NATURES BASKET", "Groceries"),

        // ================================
        // SHOPPING
        // ================================
        DefaultRule("AMAZON", "Shopping"),
        DefaultRule("FLIPKART", "Shopping"),
        DefaultRule("MYNTRA", "Shopping"),
        DefaultRule("AJIO", "Shopping"),
        DefaultRule("NYKAA", "Shopping"),
        DefaultRule("MEESHO", "Shopping"),
        DefaultRule("TATACLIQ", "Shopping"),
        DefaultRule("CROMA", "Shopping"),
        DefaultRule("RELIANCE DIGITAL", "Shopping"),
        DefaultRule("VIJAY SALES", "Shopping"),
        DefaultRule("DECATHLON", "Shopping"),
        DefaultRule("IKEA", "Shopping"),
        DefaultRule("FIRSTCRY", "Shopping"),

        // ================================
        // TRANSPORT
        // ================================
        DefaultRule("UBER", "Cabs"),
        DefaultRule("OLA", "Cabs"),
        DefaultRule("RAPIDO", "Cabs"),
        DefaultRule("NAMMA METRO", "Cabs"),
        DefaultRule("BMTC", "Cabs"),

        // ================================
        // TRAVEL
        // ================================
        DefaultRule("IRCTC", "Travel"),
        DefaultRule("REDBUS", "Travel"),
        DefaultRule("ABHIBUS", "Travel"),
        DefaultRule("MAKEMYTRIP", "Travel"),
        DefaultRule("GOIBIBO", "Travel"),
        DefaultRule("YATRA", "Travel"),
        DefaultRule("AIR INDIA", "Travel"),
        DefaultRule("INDIGO", "Travel"),
        DefaultRule("AKASA", "Travel"),

        // ================================
        // FUEL
        // ================================
        DefaultRule("BPCL", "Fuel"),
        DefaultRule("HPCL", "Fuel"),
        DefaultRule("IOC", "Fuel"),
        DefaultRule("INDIAN OIL", "Fuel"),
        DefaultRule("BHARAT PETROLEUM", "Fuel"),
        DefaultRule("SHELL", "Fuel"),
        DefaultRule("NAYARA", "Fuel"),

        // ================================
        // HEALTHCARE
        // ================================
        DefaultRule("APOLLO PHARMACY", "Healthcare"),
        DefaultRule("APOLLO HOSPITAL", "Healthcare"),
        DefaultRule("MEDPLUS", "Healthcare"),
        DefaultRule("1MG", "Healthcare"),
        DefaultRule("PHARMEASY", "Healthcare"),
        DefaultRule("FORTIS", "Healthcare"),
        DefaultRule("MANIPAL", "Healthcare"),
        DefaultRule("NARAYANA", "Healthcare"),
        DefaultRule("MAX HOSPITAL", "Healthcare"),

        // ================================
        // EDUCATION
        // ================================
        DefaultRule("UNACADEMY", "Education"),
        DefaultRule("BYJUS", "Education"),
        DefaultRule("PW", "Education"),
        DefaultRule("PHYSICS WALLAH", "Education"),
        DefaultRule("UDEMY", "Education"),
        DefaultRule("COURSERA", "Education"),
        DefaultRule("UPGRAD", "Education"),
        DefaultRule("SCHOOL", "Education"),
        DefaultRule("COLLEGE", "Education"),
        DefaultRule("UNIVERSITY", "Education"),

        // ================================
        // BILLS
        // ================================
        DefaultRule("BESCOM", "Bills"),
        DefaultRule("BWSSB", "Bills"),
        DefaultRule("AIRTEL", "Bills"),
        DefaultRule("JIO", "Bills"),
        DefaultRule("VI", "Bills"),
        DefaultRule("BSNL", "Bills"),
        DefaultRule("ACT FIBERNET", "Bills"),
        DefaultRule("HATHWAY", "Bills"),
        DefaultRule("TATAPLAY", "Bills"),

        // ================================
        // ENTERTAINMENT
        // ================================
        DefaultRule("NETFLIX", "Entertainment"),
        DefaultRule("SPOTIFY", "Entertainment"),
        DefaultRule("PRIME VIDEO", "Entertainment"),
        DefaultRule("HOTSTAR", "Entertainment"),
        DefaultRule("JIOHOTSTAR", "Entertainment"),
        DefaultRule("SONY LIV", "Entertainment"),
        DefaultRule("ZEE5", "Entertainment"),
        DefaultRule("BOOKMYSHOW", "Entertainment"),
        DefaultRule("PVR", "Entertainment"),
        DefaultRule("INOX", "Entertainment"),

        // ================================
        // INVESTMENTS
        // ================================
        DefaultRule("ZERODHA", "Investments"),
        DefaultRule("GROWW", "Investments"),
        DefaultRule("UPSTOX", "Investments"),
        DefaultRule("ANGEL ONE", "Investments"),
        DefaultRule("PAYTM MONEY", "Investments"),
        DefaultRule("SMALLCASE", "Investments"),
        DefaultRule("KUVERA", "Investments"),
        DefaultRule("MUTUAL FUND", "Investments"),
        DefaultRule("SIP", "Investments"),
        DefaultRule("NPS", "Investments"),

        // ================================
        // INSURANCE
        // ================================
        DefaultRule("LIC", "Insurance"),
        DefaultRule("ACKO", "Insurance"),
        DefaultRule("STAR HEALTH", "Insurance"),
        DefaultRule("HDFC ERGO", "Insurance"),
        DefaultRule("ICICI LOMBARD", "Insurance"),
        DefaultRule("NIVA BUPA", "Insurance"),
        DefaultRule("BAJAJ ALLIANZ", "Insurance"),

        // ================================
        // LOANS & EMI
        // ================================
        DefaultRule("EMI", "Loan & EMI"),
        DefaultRule("HOME LOAN", "Loan & EMI"),
        DefaultRule("PERSONAL LOAN", "Loan & EMI"),
        DefaultRule("VEHICLE LOAN", "Loan & EMI"),

        // ================================
        // RENT
        // ================================
        DefaultRule("HOUSE RENT", "Rent"),
        DefaultRule("RENT", "Rent"),
        DefaultRule("HOSTEL", "Rent"),
        DefaultRule("PG", "Rent"),

        // ================================
        // SUBSCRIPTIONS
        // ================================
        DefaultRule("GOOGLE ONE", "Subscriptions"),
        DefaultRule("APPLE", "Subscriptions"),
        DefaultRule("ICLOUD", "Subscriptions"),
        DefaultRule("MICROSOFT", "Subscriptions"),
        DefaultRule("ADOBE", "Subscriptions"),
        DefaultRule("CHATGPT", "Subscriptions"),
        DefaultRule("YOUTUBE PREMIUM", "Subscriptions"),

        // ================================
        // BANKING
        // ================================
        DefaultRule("ATM", "Bank Charges"),
        DefaultRule("ATM WDL", "Bank Charges"),
        DefaultRule("CASH WITHDRAWAL", "Bank Charges"),
        DefaultRule("SERVICE CHARGE", "Bank Charges"),
        DefaultRule("ANNUAL FEE", "Bank Charges"),
        DefaultRule("LATE FEE", "Bank Charges"),

        // ================================
        // TAX
        // ================================
        DefaultRule("INCOME TAX", "Taxes"),
        DefaultRule("GST", "Taxes"),
        DefaultRule("PROPERTY TAX", "Taxes"),

        // ================================
        // DONATION
        // ================================
        DefaultRule("DONATION", "Donations"),
        DefaultRule("CHARITY", "Donations"),
        DefaultRule("TEMPLE", "Donations"),
        DefaultRule("TRUST", "Donations"),

        // ================================
        // TRANSFERS (Ignored in reports)
        // ================================
        DefaultRule("TRANSFER", "Transfer"),
        DefaultRule("SELF TRANSFER", "Transfer"),
        DefaultRule("ACCOUNT TRANSFER", "Transfer"),

        DefaultRule("BANK TRANSFER", "Bank Transfer"),
        DefaultRule("NEFT", "Bank Transfer"),
        DefaultRule("RTGS", "Bank Transfer"),
        DefaultRule("IMPS", "Bank Transfer"),

        DefaultRule("UPI", "UPI Transfer"),
        DefaultRule("UPI PAYMENT", "UPI Transfer"),
        DefaultRule("UPI TRANSFER", "UPI Transfer"),

        DefaultRule("PAYTM WALLET", "Wallet Transfer"),
        DefaultRule("PHONEPE WALLET", "Wallet Transfer"),
        DefaultRule("AMAZON PAY", "Wallet Transfer"),
        DefaultRule("MOBIKWIK", "Wallet Transfer"),

        DefaultRule("ATM WDL", "Cash Withdrawal"),
        DefaultRule("ATM WITHDRAWAL", "Cash Withdrawal"),
        DefaultRule("CASH WDL", "Cash Withdrawal"),
        DefaultRule("CASH WITHDRAWAL", "Cash Withdrawal"),

        DefaultRule("CASH DEPOSIT", "Cash Deposit"),
        DefaultRule("CDM", "Cash Deposit"),
        DefaultRule("BRANCH CASH", "Cash Deposit"),

        DefaultRule("CREDIT CARD PAYMENT", "Credit Card Payment"),
        DefaultRule("CC PAYMENT", "Credit Card Payment"),
        DefaultRule("CARD PAYMENT", "Credit Card Payment")
    )
}