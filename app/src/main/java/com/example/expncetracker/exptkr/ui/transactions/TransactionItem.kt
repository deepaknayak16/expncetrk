package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
fun TransactionListItem(transaction: Transaction) {
    val isDarkTheme = MaterialTheme.isDark
    val (icon, color) = getTransactionStyle(transaction.category, isDarkTheme)
    val isExpense = transaction.type == TransactionType.DEBIT

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background // Solid background to prevent overlap with swipe content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Merchant and Account Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (transaction.bankName.contains("Card", ignoreCase = true)) Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Amount
            Text(
                text = (if (isExpense) "-" else "+") + transaction.amount.formatAsCurrency(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun TransactionItemImproved(transaction: Transaction) {
    val isDarkTheme = MaterialTheme.isDark
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
        "HEALTHCARE" -> Icons.Default.Favorite to if (isDarkTheme) CategoryHealthDark else CategoryHealth
        "EDUCATION" -> Icons.Default.School to if (isDarkTheme) CategoryEducationDark else CategoryEducation
        "GROCERIES" -> Icons.Default.ShoppingCart to if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        else -> Icons.Default.GridView to if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}
