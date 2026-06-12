package com.example.expncetracker.exptkr.core.parser

import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import java.util.Locale

object CategoryDetector {
    fun detect(merchant: String, type: TransactionType): Category {
        val upper = merchant.uppercase(Locale.ROOT)
        
        if (type == TransactionType.CREDIT) {
            if (upper.contains("SALARY") || upper.contains("WIPRO") || upper.contains("INFOSYS") || upper.contains("PAYROLL") || upper.contains("HCL") || upper.contains("TCS")) return Category.SALARY
        }

        return when {
            upper.contains("SWIGGY") || upper.contains("ZOMATO") || upper.contains("RESTAU") || upper.contains("FOOD") || upper.contains("CAFE") || upper.contains("HOTEL") || upper.contains("EATS") -> Category.FOOD
            upper.contains("UBER") || upper.contains("OLA") || upper.contains("RAPIDO") || upper.contains("NAMA") || upper.contains("METRO") || upper.contains("TAXI") -> Category.CABS
            upper.contains("RENT") || upper.contains("NOBROKER") || upper.contains("HOUSING") -> Category.RENT
            upper.contains("BESCOM") || upper.contains("AIRTEL") || upper.contains("JIO") || upper.contains("RECHARGE") || upper.contains("BILL") || upper.contains("ELECTRICITY") || upper.contains("WATER") || upper.contains("GAS") -> Category.BILLS
            upper.contains("AMAZON") || upper.contains("FLIPKART") || upper.contains("DMART") || upper.contains("BLINKIT") || upper.contains("INSTAMART") || upper.contains("SHOP") || upper.contains("STORE") || upper.contains("MYNTRA") || upper.contains("AJIO") || upper.contains("MALL") -> Category.SHOPPING
            upper.contains("ZERODHA") || upper.contains("GROWW") || upper.contains("MUTUAL") || upper.contains("INVEST") || upper.contains("STOCK") || upper.contains("BROKER") || upper.contains("COIN") -> Category.INVESTMENTS
            upper.contains("AIRLINES") || upper.contains("FLIGHT") || upper.contains("INDIGO") || upper.contains("RAILWAY") || upper.contains("IRCTC") || upper.contains("TRAVEL") || upper.contains("MAKEMYTRIP") || upper.contains("GOIBIBO") -> Category.TRAVEL
            upper.contains("NETFLIX") || upper.contains("PRIME") || upper.contains("HOTSTAR") || upper.contains("CINEMA") || upper.contains("MOVIE") || upper.contains("BOOKMYSHOW") || upper.contains("PVR") || upper.contains("INOX") -> Category.ENTERTAINMENT
            upper.contains("BIGBASKET") || upper.contains("ZEPTO") || upper.contains("GROCERY") || upper.contains("MILK") || upper.contains("RETAIL") || upper.contains("SUPERMARKET") -> Category.GROCERIES
            upper.contains("HOSPITAL") || upper.contains("PHARMACY") || upper.contains("DR ") || upper.contains("CLINIC") || upper.contains("MEDPLUS") || upper.contains("APOLLO") || upper.contains("HEALTH") -> Category.HEALTHCARE
            upper.contains("SCHOOL") || upper.contains("COLLEGE") || upper.contains("FEE") || upper.contains("UNIVERSITY") || upper.contains("UDEMY") || upper.contains("COURSERA") || upper.contains("EDUCATION") -> Category.EDUCATION
            else -> Category.OTHERS
        }
    }
}
