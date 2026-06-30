package com.example.expncetracker.exptkr.ui.categories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.components.getCategoryIcon
import com.example.expncetracker.exptkr.ui.components.availableIcons
import com.example.expncetracker.exptkr.ui.components.presetColors
import com.example.expncetracker.exptkr.ui.components.GradientCard
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.ui.dashboard.FilterItemCompact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
    val isDark = MaterialTheme.isDark
    val summary by viewModel.summary.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val currentViewDate by viewModel.selectedDate.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val incomeCategories = allCategories
        .filter { it.type.uppercase() == "INCOME" }
        .sortedByDescending { summary?.incomeCategoryDistribution?.get(it.name) ?: BigDecimal.ZERO }
    
    val expenseCategories = allCategories
        .filter { it.type.uppercase() == "EXPENSE" }
        .sortedByDescending { summary?.categoryDistribution?.get(it.name) ?: BigDecimal.ZERO }
    
    val totalExpenseVolume = summary?.totalExpense ?: BigDecimal.ZERO
    val totalIncomeVolume = summary?.totalIncome ?: BigDecimal.ZERO

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Date Selector (Added to see June 2026 data)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterItemCompact(DateFilter.DAY, currentFilter, { viewModel.setFilter(it) }, Modifier.weight(1f))
                        FilterItemCompact(DateFilter.WEEK, currentFilter, { viewModel.setFilter(it) }, Modifier.weight(1f))

                        Row(
                            modifier = Modifier.weight(2.5f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = {
                                val nextDate = if (currentFilter == DateFilter.YEAR) currentViewDate.minusYears(1) else currentViewDate.minusMonths(1)
                                viewModel.updateDate(nextDate)
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronLeft, "Prev", modifier = Modifier.size(20.dp))
                            }

                            Text(
                                text = if (currentFilter == DateFilter.YEAR) currentViewDate.year.toString()
                                else currentViewDate.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(onClick = {
                                val nextDate = if (currentFilter == DateFilter.YEAR) currentViewDate.plusYears(1) else currentViewDate.plusMonths(1)
                                viewModel.updateDate(nextDate)
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronRight, "Next", modifier = Modifier.size(20.dp))
                            }
                        }

                        FilterItemCompact(DateFilter.MONTH, currentFilter, { viewModel.setFilter(it) }, Modifier.weight(1f))
                        FilterItemCompact(DateFilter.YEAR, currentFilter, { viewModel.setFilter(it) }, Modifier.weight(1f))
                    }
                }

                // Enhanced Summary Card
                GradientCard(
                    title = "Total Financial Health",
                    value = summary?.balance?.formatAsCurrency() ?: "₹0.00",
                    gradientStart = if (isDark) Color(0xFF1A237E) else MaterialTheme.colorScheme.primary,
                    gradientEnd = if (isDark) Color(0xFF0D47A1) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    icon = Icons.Default.AccountBalance
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryColumnItem(
                            label = "TOTAL INCOME",
                            amount = summary?.totalIncome?.formatAsCurrency() ?: "₹0.00",
                            color = Color.White
                        )
                        SummaryColumnItem(
                            label = "TOTAL EXPENSE",
                            amount = summary?.totalExpense?.formatAsCurrency() ?: "₹0.00",
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Two Column Layout for Categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Income
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySectionHeader("Income", if (isDark) DarkIncome else LightIncome, Icons.Default.TrendingUp)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (incomeCategories.isEmpty()) {
                            EmptyCategoryPlaceholder("No income categories")
                        } else {
                            incomeCategories.forEach { category ->
                                CompactCategoryItem(
                                    category = category,
                                    amount = summary?.incomeCategoryDistribution?.get(category.name) ?: BigDecimal.ZERO,
                                    totalAmount = totalIncomeVolume,
                                    isDark = isDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Right Column: Expense
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySectionHeader("Expense", if (isDark) DarkExpense else LightExpense, Icons.Default.TrendingDown)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (expenseCategories.isEmpty()) {
                            EmptyCategoryPlaceholder("No expense categories")
                        } else {
                            expenseCategories.forEach { category ->
                                CompactCategoryItem(
                                    category = category,
                                    amount = summary?.categoryDistribution?.get(category.name) ?: BigDecimal.ZERO,
                                    totalAmount = totalExpenseVolume,
                                    isDark = isDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            existingCategories = allCategories.map { it.name },
            onDismiss = { viewModel.onDialogDismissed() },
            onConfirm = { name, type, iconName, color ->
                viewModel.addCategory(name, type, iconName, color)
                viewModel.onDialogDismissed()
            }
        )
    }
}

@Composable
private fun SummaryColumnItem(label: String, amount: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun CategorySectionHeader(title: String, color: Color, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CompactCategoryItem(
    category: CategoryEntity,
    amount: BigDecimal,
    totalAmount: BigDecimal,
    isDark: Boolean
) {
    val categoryEnum = Category.entries.find { it.name == category.iconName } ?: Category.OTHERS
    val percentage = if (totalAmount > BigDecimal.ZERO) {
        (amount.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toInt()
    } else 0
    val color = Color(category.color)
    val amountColor = when (category.type.uppercase()) {
        "INCOME" -> if (isDark) DarkIncome else LightIncome
        else -> if (isDark) DarkExpense else LightExpense
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(categoryEnum),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = amount.formatAsCurrency(),
                    style = MaterialTheme.typography.labelLarge,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                if (percentage > 0) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (percentage > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { percentage.toFloat() / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
private fun EmptyCategoryPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var selectedIconName by remember { mutableStateOf("OTHERS") }
    var selectedColor by remember { mutableStateOf(presetColors[0]) }
    
    val isNameDuplicate = existingCategories.any { it.equals(name.trim(), ignoreCase = true) }

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
                    shape = MaterialTheme.shapes.medium,
                    isError = isNameDuplicate && name.isNotBlank(),
                    supportingText = {
                        if (isNameDuplicate && name.isNotBlank()) {
                            Text("Category name already exists")
                        }
                    }
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
                
                Spacer(Modifier.height(16.dp))
                Text("Select Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableIcons) { (iconName, icon) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selectedIconName == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIconName = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selectedIconName == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Select Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { colorValue ->
                        val color = Color(colorValue)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (selectedColor == colorValue) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { selectedColor = colorValue }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && !isNameDuplicate) onConfirm(name.trim(), type, selectedIconName, selectedColor.toInt()) },
                enabled = name.isNotBlank() && !isNameDuplicate,
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
