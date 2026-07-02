package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    categories: List<Category>,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Requirement 3: Dynamic lookup from ViewModel's state
    // FIX: Case-insensitive lookup to handle "others" vs "Others" mismatches
    val categoryMetadata = categories.find { it.id.equals(transaction.categoryName, ignoreCase = true) }
    
    // Fallback UI
    val displayName = categoryMetadata?.name ?: "Others"
    val displayColor = categoryMetadata?.color.toComposeColor()
    val iconString = categoryMetadata?.icon ?: "help_outline"

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
        TransactionType.CREDIT, TransactionType.BORROW -> Color(0xFF2E7D32)
        TransactionType.DEBIT, TransactionType.LEND -> MaterialTheme.colorScheme.error
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
                    color = displayColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, displayColor.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconByName(iconString),
                            contentDescription = displayName,
                            tint = displayColor,
                            modifier = Modifier.size(24.dp)
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
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = displayName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
