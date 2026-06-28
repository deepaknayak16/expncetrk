package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
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

    val typeColor = when (transaction.type) {
        TransactionType.CREDIT, TransactionType.BORROW -> Color(0xFF2E7D32) // Success Green
        TransactionType.DEBIT, TransactionType.LEND -> MaterialTheme.colorScheme.error
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null || onEdit != null) { 
                if (onClick != null) onClick() else onEdit?.invoke() 
            },
        color = Color.Transparent
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val categoryLabel = transaction.categoryName
                        val isOthers = categoryLabel.trim().lowercase().let { it == "other" || it == "others" }
                        
                        Surface(
                            color = if (isOthers) MaterialTheme.colorScheme.errorContainer 
                                    else MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = categoryLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOthers) MaterialTheme.colorScheme.onErrorContainer 
                                        else MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (transaction.counterparty != null) {
                            Surface(
                                modifier = Modifier.padding(start = 6.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = transaction.counterparty,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                val prefix = when (transaction.type) {
                    TransactionType.CREDIT -> "+"
                    TransactionType.DEBIT -> "-"
                    TransactionType.TRANSFER -> "="
                    TransactionType.LEND -> "↗"
                    TransactionType.BORROW -> "↙"
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix ${transaction.amount.formatAsCurrency()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = typeColor
                    )
                    Text(
                        text = transaction.timestamp.formatToDisplay(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.padding(start = 8.dp).size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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
