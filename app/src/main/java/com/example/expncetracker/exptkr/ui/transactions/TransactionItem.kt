package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.expncetracker.exptkr.ui.components.getIconByName

@Composable
fun TransactionListItem(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    categoryIcon: ImageVector? = null,
    categoryColor: Color? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.isDark
    
    // Fallback if category name doesn't match enum
    val categoryEnum = Category.entries.find { it.name == transaction.categoryName.uppercase() } ?: Category.OTHERS
    val style = remember(categoryEnum, isDarkTheme) { 
        getTransactionStyle(categoryEnum, isDarkTheme) 
    }
    
    val icon = categoryIcon ?: style.first
    val color = categoryColor ?: style.second

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
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null || onEdit != null) { 
                if (onClick != null) onClick() else onEdit?.invoke() 
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    val detailText = buildString {
                        append(transaction.categoryName)
                        if (transaction.counterparty != null) {
                            append(" • ${transaction.counterparty}")
                        }
                        append(" • ${transaction.timestamp.formatToDisplay()}")
                    }
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val prefix = when (transaction.type) {
                    TransactionType.CREDIT -> "+"
                    TransactionType.DEBIT -> "-"
                    TransactionType.TRANSFER -> "⇄ "
                    TransactionType.LEND -> "↗ "
                    TransactionType.BORROW -> "↙ "
                }
                val amountColor = when (transaction.type) {
                    TransactionType.CREDIT, TransactionType.BORROW -> if (isDarkTheme) DarkIncome else LightIncome
                    TransactionType.DEBIT, TransactionType.LEND -> if (isDarkTheme) DarkExpense else LightExpense
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

private fun getTransactionStyle(category: Category, isDarkTheme: Boolean): Pair<ImageVector, Color> {
    return when (category) {
        Category.FOOD -> Icons.Default.Restaurant to if (isDarkTheme) CategoryFoodDark else CategoryFood
        Category.CABS -> Icons.Default.DirectionsCar to if (isDarkTheme) CategoryCabsDark else CategoryCabs
        Category.RENT -> Icons.Default.HomeWork to if (isDarkTheme) CategoryRentDark else CategoryRent
        Category.BILLS -> Icons.Default.Bolt to if (isDarkTheme) CategoryBillsDark else CategoryBills
        Category.SHOPPING -> Icons.Default.LocalMall to if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.SALARY -> Icons.Default.Payments to if (isDarkTheme) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS -> Icons.AutoMirrored.Filled.TrendingUp to if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL -> Icons.Default.LocalAirport to if (isDarkTheme) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> Icons.Default.LiveTv to if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES -> Icons.Default.ShoppingCart to if (isDarkTheme) CategoryGroceriesDark else CategoryGroceries
        Category.HEALTHCARE -> Icons.Default.Favorite to if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> Icons.Default.School to if (isDarkTheme) CategoryEducationDark else CategoryEducation
        else -> Icons.Default.GridView to if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}
