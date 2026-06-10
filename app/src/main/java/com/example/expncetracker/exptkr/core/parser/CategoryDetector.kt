package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import java.util.Locale

object CategoryDetector {
    fun detect(merchant: String, type: TransactionType): Category {
        val upper = merchant.uppercase(Locale.ROOT)
        
        if (type == TransactionType.CREDIT) {
            if (upper.contains("SALARY") || upper.contains("WIPRO") || upper.contains("INFOSYS") || upper.contains("PAYROLL")) return Category.SALARY
        }

        return when {
            upper.contains("SWIGGY") || upper.contains("ZOMATO") || upper.contains("RESTAU") || upper.contains("FOOD") || upper.contains("CAFE") || upper.contains("HOTEL") -> Category.FOOD
            upper.contains("UBER") || upper.contains("OLA") || upper.contains("RAPIDO") || upper.contains("NAMA") || upper.contains("METRO") || upper.contains("TAXI") -> Category.CABS
            upper.contains("RENT") || upper.contains("NOBROKER") || upper.contains("HOUSING") -> Category.RENT
            upper.contains("BESCOM") || upper.contains("AIRTEL") || upper.contains("JIO") || upper.contains("RECHARGE") || upper.contains("BILL") || upper.contains("ELECTRICITY") || upper.contains("WATER") -> Category.BILLS
            upper.contains("AMAZON") || upper.contains("FLIPKART") || upper.contains("DMART") || upper.contains("BLINKIT") || upper.contains("INSTAMART") || upper.contains("SHOP") || upper.contains("STORE") || upper.contains("MYNTRA") -> Category.SHOPPING
            upper.contains("ZERODHA") || upper.contains("GROWW") || upper.contains("MUTUAL") || upper.contains("INVEST") || upper.contains("STOCK") || upper.contains("BROKER") -> Category.INVESTMENTS
            upper.contains("AIRLINES") || upper.contains("FLIGHT") || upper.contains("INDIGO") || upper.contains("RAILWAY") || upper.contains("IRCTC") || upper.contains("TRAVEL") -> Category.TRAVEL
            else -> Category.OTHERS
        }
    }
}
