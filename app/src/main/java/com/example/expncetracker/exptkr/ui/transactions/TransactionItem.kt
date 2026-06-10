package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.theme.*

@Composable
fun TransactionItemImproved(transaction: Transaction) {
    val isDarkTheme = MaterialTheme.colorScheme.isDark()
    val (icon, color) = getTransactionStyle(transaction.category, isDarkTheme)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with larger container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Merchant & Category Info
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.category.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"
                val amountColor = if (transaction.type == TransactionType.CREDIT) {
                    if (isDarkTheme) DarkIncome else LightIncome
                } else {
                    if (isDarkTheme) DarkExpense else LightExpense
                }
                Text(
                    text = "$prefix ${transaction.amount.formatAsCurrency()}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.bankName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun getTransactionStyle(category: Category, isDarkTheme: Boolean): Pair<ImageVector, Color> {
    return when (category.name) {
        "FOOD" -> Icons.Default.Restaurant to if (isDarkTheme) CategoryFoodDark else CategoryFood
        "CABS" -> Icons.AutoMirrored.Filled.DirectionsRun to if (isDarkTheme) CategoryCabsDark else CategoryCabs
        "BILLS" -> Icons.Default.Bolt to if (isDarkTheme) CategoryBillsDark else CategoryBills
        "SHOPPING" -> Icons.Default.LocalMall to if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        "SALARY" -> Icons.Default.Payments to if (isDarkTheme) CategorySalaryDark else CategorySalary
        "RENT" -> Icons.Default.HomeWork to if (isDarkTheme) CategoryRentDark else CategoryRent
        "INVESTMENTS" -> Icons.AutoMirrored.Filled.TrendingUp to if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        "TRAVEL" -> Icons.Default.LocalAirport to if (isDarkTheme) CategoryTravelDark else CategoryTravel
        "ENTERTAINMENT" -> Icons.Default.LiveTv to if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        "HEALTH" -> Icons.Default.Favorite to if (isDarkTheme) CategoryHealthDark else CategoryHealth
        "EDUCATION" -> Icons.Default.School to if (isDarkTheme) CategoryEducationDark else CategoryEducation
        else -> Icons.Default.GridView to if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}