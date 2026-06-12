package com.example.expncetracker.exptkr.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.ui.theme.*

// Reusing colors from AddTransaction for consistency
private val CalculatorBg = Color(0xFFFEFBEA)
private val CalculatorGreen = Color(0xFF2D5D4E)

@Composable
fun CategoriesScreen() {
    val incomeCategories = listOf(
        Category.SALARY,
        Category.INVESTMENTS,
        Category.OTHERS
    )
    
    val expenseCategories = Category.entries.filter { it !in incomeCategories }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatorBg)
    ) {
        // Summary Header (matching image style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "[ All Accounts ₹9,096.28 ]",
                color = CalculatorGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("EXPENSE SO FAR", "₹4,853.72", Color(0xFFD32F2F))
                SummaryItem("INCOME SO FAR", "₹8,700.00", Color(0xFF388E3C))
            }
        }

        HorizontalDivider(color = CalculatorGreen.copy(alpha = 0.2f))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                CategoryHeader("Income categories")
            }
            items(incomeCategories) { category ->
                CategoryListItem(category)
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = CalculatorGreen.copy(alpha = 0.1f))
            }
            
            item {
                CategoryHeader("Expense categories")
            }
            items(expenseCategories) { category ->
                CategoryListItem(category)
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = CalculatorGreen.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = CalculatorGreen.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        color = CalculatorGreen,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun CategoryListItem(category: Category) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(getCategoryColor(category)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            text = category.displayName,
            modifier = Modifier.weight(1f),
            color = CalculatorGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Options",
            tint = CalculatorGreen.copy(alpha = 0.5f)
        )
    }
}

private fun getCategoryIcon(category: Category): ImageVector {
    return when (category) {
        Category.FOOD -> Icons.Default.Restaurant
        Category.CABS -> Icons.Default.DirectionsCar
        Category.RENT -> Icons.Default.HomeWork
        Category.BILLS -> Icons.Default.Bolt
        Category.SHOPPING -> Icons.Default.LocalMall
        Category.SALARY -> Icons.Default.Payments
        Category.INVESTMENTS -> Icons.AutoMirrored.Filled.TrendingUp
        Category.TRAVEL -> Icons.Default.LocalAirport
        Category.ENTERTAINMENT -> Icons.Default.LiveTv
        Category.HEALTHCARE -> Icons.Default.Favorite
        Category.EDUCATION -> Icons.Default.School
        Category.GROCERIES -> Icons.Default.ShoppingCart
        Category.OTHERS -> Icons.Default.GridView
    }
}

private fun getCategoryColor(category: Category): Color {
    return when (category) {
        Category.SALARY -> Color(0xFF1E88E5)
        Category.SHOPPING -> Color(0xFFD81B60)
        Category.INVESTMENTS -> Color(0xFF8E24AA)
        Category.FOOD -> Color(0xFFF4511E)
        Category.BILLS -> Color(0xFF43A047)
        Category.RENT -> Color(0xFF795548)
        Category.TRAVEL -> Color(0xFF00ACC1)
        else -> Color(0xFF546E7A)
    }
}
