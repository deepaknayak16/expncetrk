package com.example.expncetracker.exptkr.core.common

object Constants {
    const val DATABASE_NAME = "expense_tracker_db"
    const val BACKUP_FILE_NAME = "expense_tracker_backup.json"
    const val FILE_PROVIDER_AUTHORITY = "com.example.expncetracker.fileprovider"
    val BANK_SENDERS = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "CANBNK", "IDBI", 
        "UNION", "YESBNK", "RBL", "INDUS", "HSBC", "STAN", "FED", "UBI",
        "CITI", "BANK"
    )
    val WALLET_SENDERS = listOf("PAYTM")
}
