package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.core.common.formatToDisplay
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.theme.*

/**
 * Unified Transaction List Item used across Home and Transaction screens.
 */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.isDark
    val (icon, color) = getTransactionStyle(transaction.category, isDarkTheme)
    val isExpense = transaction.type == TransactionType.DEBIT

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction for ${transaction.merchant}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Merchant & Category Info
                Column(Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${transaction.category.displayName} • ${transaction.timestamp.formatToDisplay()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                val prefix = when (transaction.type) {
                    TransactionType.CREDIT -> "+"
                    TransactionType.DEBIT -> "-"
                    TransactionType.TRANSFER -> "⇄ "
                }
                val amountColor = when (transaction.type) {
                    TransactionType.CREDIT -> if (isDarkTheme) DarkIncome else LightIncome
                    TransactionType.DEBIT -> if (isDarkTheme) DarkExpense else LightExpense
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$prefix${transaction.amount.formatAsCurrency()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = amountColor
                    )
                    
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun TransactionItemImproved(transaction: Transaction) {
    TransactionListItem(transaction = transaction)
}

private fun getTransactionStyle(category: Category, isDarkTheme: Boolean): Pair<ImageVector, Color> {
    return when (category.name) {
        "FOOD" -> Icons.Default.Restaurant to if (isDarkTheme) CategoryFoodDark else CategoryFood
        "CABS" -> Icons.Default.DirectionsCar to if (isDarkTheme) CategoryCabsDark else CategoryCabs
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
