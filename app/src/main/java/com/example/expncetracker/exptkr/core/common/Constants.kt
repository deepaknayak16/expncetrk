package com.example.expncetracker.exptkr.core.common

object Constants {
    const val DATABASE_NAME = "expense_tracker_db"
    const val BACKUP_FILE_NAME = "expense_tracker_backup.json"
    const val FILE_PROVIDER_AUTHORITY = "com.example.expncetracker.exptkr.fileprovider"
    val BANK_SENDERS = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "CANBNK", "IDBI", 
        "UNION", "YESBNK", "RBL", "INDUS", "HSBC", "STAN", "FED", "UBI",
        "CITI", "BOB", "CANARA", "IDFC", "BANDHAN", "BOI", "CBoi", "IOB",
        "KBL", "KVB", "LVB", "OBC", "PSB", "SCB", "SIB", "TMB", "VYSYA"
    )
    val WALLET_SENDERS = listOf("PAYTM", "PHONEPE", "MOBIKWIK", "FREECHARGE", "GOOGLEPAY", "GPAY")
}
