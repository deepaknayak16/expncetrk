package com.example.expncetracker.exptkr.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.theme.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val budgetList by viewModel.budgetList.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val isDark = MaterialTheme.isDark
    
    var editingBudget by remember { mutableStateOf<BudgetUiModel?>(null) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.getDefault()) }

    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val totalBudget = budgetList.sumOf { it.limit }
    val totalSpent = budgetList.sumOf { it.spent }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.refreshBudgets()
            },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.setMonth(selectedMonth.minusMonths(1)) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Text(
                                    text = selectedMonth.format(monthFormatter),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                
                                IconButton(onClick = { viewModel.setMonth(selectedMonth.plusMonths(1)) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            BudgetStatItem("Total Budget", totalBudget.formatAsCurrency(), MaterialTheme.colorScheme.primary)
                            BudgetStatItem("Total Spent", totalSpent.formatAsCurrency(), if (isDark) Color(0xFFE57373) else Color(0xFFD32F2F))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Budgeted categories",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (budgetList.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.AccountBalance,
                                title = "No budgets set",
                                description = "Add a monthly limit for categories to track your spending",
                                action = {
                                    Button(
                                        onClick = { viewModel.triggerAddBudget() },
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Set Your First Budget")
                                    }
                                }
                            )
                        }
                    } else {
                        items(budgetList, key = { it.categoryName }) { budget ->
                            BudgetItem(
                                budget = budget,
                                onEditClick = { 
                                    editingBudget = budget
                                    viewModel.triggerAddBudget()
                                },
                                onDeleteClick = { viewModel.deleteBudgetByName(budget.categoryName) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            allCategories = allCategories,
            budgetedCategories = budgetList.map { it.categoryName },
            initialBudget = editingBudget,
            onDismiss = { 
                viewModel.onDialogDismissed()
                editingBudget = null
            },
            onConfirm = { categoryName, limit ->
                viewModel.saveBudget(categoryName, limit)
                viewModel.onDialogDismissed()
                editingBudget = null
            }
        )
    }
}

@Composable
private fun BudgetStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(), 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value, 
            color = color, 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun BudgetItem(budget: BudgetUiModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.isDark
    val targetProgress by remember(budget.progress) { mutableFloatStateOf(budget.progress) }
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "budgetProgress"
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete the budget for ${budget.displayName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEditClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEditClick()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showDeleteConfirm = true
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val progressColor = when {
                budget.progress < 0.7f -> BudgetHealthy
                budget.progress < 0.9f -> BudgetWarning
                else -> BudgetOver
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${(budget.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${budget.spent.formatAsCurrency()} spent of ${budget.limit.formatAsCurrency()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${budget.remaining.formatAsCurrency()} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (budget.remaining > BigDecimal.ZERO) (if (isDark) DarkIncome else LightIncome) else (if (isDark) DarkExpense else LightExpense),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    allCategories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    budgetedCategories: List<String>,
    initialBudget: BudgetUiModel? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Double) -> Unit
) {
    var selectedCategory by remember { 
        mutableStateOf(
            if (initialBudget != null) allCategories.find { it.name == initialBudget.categoryName }
            else allCategories.firstOrNull { it.name !in budgetedCategories }
        ) 
    }
    var limit by remember { 
        mutableStateOf(if (initialBudget != null) initialBudget.limit.toString() else "") 
    }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget != null) "Edit Budget" else "Set Monthly Budget", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedCategory != null) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (initialBudget == null) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory!!.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { if (initialBudget == null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        if (initialBudget == null) {
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                allCategories.forEach { category ->
                                    val isBudgeted = category.name in budgetedCategories
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = if (isBudgeted) "${category.name} (Budgeted)" else category.name,
                                                color = if (isBudgeted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                            ) 
                                        },
                                        onClick = {
                                            if (!isBudgeted) {
                                                selectedCategory = category
                                                expanded = false
                                            }
                                        },
                                        enabled = !isBudgeted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "All categories already have a budget. You can edit existing budgets by clicking the 'Edit' option on a category card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = limit,
                    onValueChange = { input ->
                        if (input.count { it == '.' } <= 1 && input.all { c -> c.isDigit() || c == '.' }) {
                            limit = input
                        }
                    },
                    label = { Text("Monthly Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") },
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = limit.toDoubleOrNull() ?: 0.0
                    if (l > 0 && selectedCategory != null) onConfirm(selectedCategory!!.name, l)
                },
                enabled = limit.isNotEmpty() && selectedCategory != null,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
