package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
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
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary
import com.example.expncetracker.exptkr.ui.theme.LightBorder

@Composable
fun TransactionItemImproved(transaction: Transaction) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LightSurface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, color) = getTransactionStyle(transaction.category)
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(transaction.merchant, color = LightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(transaction.category.displayName, color = LightTextSecondary, fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"
                val amountColor = if (transaction.type == TransactionType.CREDIT) Color(0xFF10B981) else LightTextPrimary
                Text(
                    text = "$prefix ${transaction.amount.formatAsCurrency()}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(transaction.bankName, color = LightTextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

private fun getTransactionStyle(category: Category): Pair<ImageVector, Color> {
    return when (category.name) {
        "FOOD" -> Icons.Default.Restaurant to Color(0xFFF97316)
        "CABS" -> Icons.AutoMirrored.Filled.DirectionsRun to Color(0xFF6366F1)
        "BILLS" -> Icons.Default.Bolt to Color(0xFFEC4899)
        "SHOPPING" -> Icons.Default.LocalMall to Color(0xFFA855F7)
        "SALARY" -> Icons.Default.Payments to Color(0xFF10B981)
        "RENT" -> Icons.Default.HomeWork to Color(0xFFEAB308)
        "INVESTMENTS" -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF06B6D4)
        "TRAVEL" -> Icons.Default.LocalAirport to Color(0xFF3B82F6)
        else -> Icons.Default.GridView to Color(0xFF64748B)
    }
}