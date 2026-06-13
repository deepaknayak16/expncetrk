package com.example.expncetracker.exptkr.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.ui.theme.*

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
    val isDark = MaterialTheme.isDark
    val summary by viewModel.summary.collectAsState()

    val incomeCategories = listOf(Category.SALARY, Category.INVESTMENTS, Category.OTHERS)
    val expenseCategories = Category.entries.filter { it !in incomeCategories }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Financial Health",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // FIX #7: Use real data from ViewModel
                Text(
                    text = "₹${summary?.balance?.formatAsCurrency() ?: "0.00"}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        "EXPENSE",
                        "₹${summary?.totalExpense?.formatAsCurrency() ?: "0.00"}",
                        if (isDark) DarkExpense else LightExpense
                    )
                    SummaryItem(
                        "INCOME",
                        "₹${summary?.totalIncome?.formatAsCurrency() ?: "0.00"}",
                        if (isDark) DarkIncome else LightIncome
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { CategoryHeader("Income Categories") }
            items(incomeCategories) { category ->
                CategoryListItem(category, isDark, summary?.categoryDistribution?.get(category) ?: 0.0)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            item { CategoryHeader("Expense Categories") }
            items(expenseCategories) { category ->
                CategoryListItem(category, isDark, summary?.categoryDistribution?.get(category) ?: 0.0)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(text = amount, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
    )
}

@Composable
private fun CategoryListItem(category: Category, isDark: Boolean, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(getCategoryColor(category, isDark)),
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (amount > 0) {
                Text(
                    text = "₹${amount.formatAsCurrency()}",
                    color = if (isDark) DarkExpense else LightExpense,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

private fun getCategoryColor(category: Category, isDarkTheme: Boolean): Color {
    return when (category) {
        Category.FOOD -> if (isDarkTheme) CategoryFoodDark else CategoryFood
        Category.CABS -> if (isDarkTheme) CategoryCabsDark else CategoryCabs
        Category.RENT -> if (isDarkTheme) CategoryRentDark else CategoryRent
        Category.BILLS -> if (isDarkTheme) CategoryBillsDark else CategoryBills
        Category.SHOPPING -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.SALARY -> if (isDarkTheme) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS -> if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL -> if (isDarkTheme) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.HEALTHCARE -> if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}
