package com.example.expncetracker.exptkr.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.ui.theme.*

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
    val isDark = MaterialTheme.isDark
    val summary by viewModel.summary.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()

    val incomeCategories = allCategories.filter { it.type == "INCOME" }
    val expenseCategories = allCategories.filter { it.type == "EXPENSE" }

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
                Text(
                    text = summary?.balance?.formatAsCurrency() ?: "₹0.00",
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
                        summary?.totalExpense?.formatAsCurrency() ?: "₹0.00",
                        if (isDark) DarkExpense else LightExpense
                    )
                    SummaryItem(
                        "INCOME",
                        summary?.totalIncome?.formatAsCurrency() ?: "₹0.00",
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
                val categoryEnum = Category.entries.find { it.name == category.iconName } ?: Category.OTHERS
                val amount = summary?.categoryDistribution?.get(categoryEnum) ?: 0.0
                val total = summary?.totalIncome ?: 1.0
                val percentage = if (total > 0) (amount / total * 100).toInt() else 0
                
                CategoryListItem(category.name, categoryEnum, isDark, amount, percentage)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            item { CategoryHeader("Expense Categories") }
            items(expenseCategories) { category ->
                val categoryEnum = Category.entries.find { it.name == category.iconName } ?: Category.OTHERS
                val amount = summary?.categoryDistribution?.get(categoryEnum) ?: 0.0
                val total = summary?.totalExpense ?: 1.0
                val percentage = if (total > 0) (amount / total * 100).toInt() else 0
                
                CategoryListItem(category.name, categoryEnum, isDark, amount, percentage)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.onDialogDismissed() },
            onConfirm = { name, type ->
                viewModel.addCategory(name, type)
                viewModel.onDialogDismissed()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(16.dp))
                Text("Category Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "EXPENSE", onClick = { type = "EXPENSE" })
                    Text("Expense", modifier = Modifier.clickable { type = "EXPENSE" })
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = type == "INCOME", onClick = { type = "INCOME" })
                    Text("Income", modifier = Modifier.clickable { type = "INCOME" })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, type) },
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CategoryListItem(displayName: String, category: Category, isDark: Boolean, amount: Double, percentage: Int = 0) {
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
                text = displayName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (amount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = amount.formatAsCurrency(),
                        color = if (isDark) DarkExpense else LightExpense,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$percentage%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
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
        Category.GROCERIES -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.HEALTHCARE -> if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}
