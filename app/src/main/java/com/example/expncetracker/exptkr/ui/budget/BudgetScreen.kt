package com.example.expncetracker.exptkr.ui.budget

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.ui.components.EmptyState
import com.example.expncetracker.exptkr.ui.theme.*
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Health model
// ─────────────────────────────────────────────────────────────────────────────

private enum class BudgetStatus(val label: String, val icon: ImageVector) {
    HEALTHY("On Track",    Icons.Default.CheckCircle),
    WARNING("Watch Out",   Icons.Default.Warning),
    OVER   ("Over Budget", Icons.Default.Error)
}

private fun progressStatus(p: Float) = when {
    p < 0.70f -> BudgetStatus.HEALTHY
    p < 0.90f -> BudgetStatus.WARNING
    else      -> BudgetStatus.OVER
}

private fun progressColor(p: Float) = when {
    p < 0.70f -> BudgetHealthy
    p < 0.90f -> BudgetWarning
    else      -> BudgetOver
}

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val budgetList    by viewModel.budgetList.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isRefreshing  by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val isDark = MaterialTheme.isDark

    var editingBudget    by remember { mutableStateOf<BudgetUiModel?>(null) }
    val sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pullRefreshState  = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val monthFormatter    = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    val totalBudget  = budgetList.sumOf { it.limit }
    val totalSpent   = budgetList.sumOf { it.spent }
    val rawOverall   = if (totalBudget > BigDecimal.ZERO)
        (totalSpent.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f) else 0f

    val overallProgress by animateFloatAsState(
        targetValue   = rawOverall,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "overallProgress"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = { viewModel.refreshBudgets() },
            state        = pullRefreshState,
            modifier     = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Single LazyColumn — avoids nested scrollables, fixes pull-to-refresh
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {

                // ── Overview header ──────────────────────────────────────
                item {
                    BudgetHeader(
                        selectedMonth   = selectedMonth,
                        monthFormatter  = monthFormatter,
                        totalBudget     = totalBudget,
                        totalSpent      = totalSpent,
                        overallProgress = overallProgress,
                        onPrev          = { viewModel.setMonth(selectedMonth.minusMonths(1)) },
                        onNext          = { viewModel.setMonth(selectedMonth.plusMonths(1)) }
                    )
                }

                // ── Section label ────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Category Budgets",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        if (budgetList.isNotEmpty()) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text       = "${budgetList.size}",
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // ── Empty state ──────────────────────────────────────────
                if (budgetList.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon        = Icons.Default.AccountBalance,
                                title       = "No budgets yet",
                                description = "Set monthly limits per category to keep spending in check.",
                                action = {
                                    Button(
                                        onClick = { viewModel.triggerAddBudget() },
                                        shape   = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Set Your First Budget")
                                    }
                                }
                            )
                        }
                    }
                } else {
                    items(budgetList, key = { it.categoryName }) { budget ->
                        BudgetItemCard(
                            budget        = budget,
                            isDark        = isDark,
                            onEditClick   = { editingBudget = budget; viewModel.triggerAddBudget() },
                            onDeleteClick = { viewModel.deleteBudgetByName(budget.categoryName) },
                            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Add / Edit bottom sheet ──────────────────────────────────────────────
    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDialogDismissed(); editingBudget = null },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            dragHandle       = { BottomSheetDefaults.DragHandle() }
        ) {
            AddBudgetSheetContent(
                allCategories      = allCategories,
                budgetedCategories = budgetList.map { it.categoryName },
                initialBudget      = editingBudget,
                onDismiss          = { viewModel.onDialogDismissed(); editingBudget = null },
                onConfirm          = { name, limit ->
                    viewModel.saveBudget(name, limit)
                    viewModel.onDialogDismissed()
                    editingBudget = null
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview header — replaces GradientCard with a clean spending summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetHeader(
    selectedMonth   : YearMonth,
    monthFormatter  : DateTimeFormatter,
    totalBudget     : BigDecimal,
    totalSpent      : BigDecimal,
    overallProgress : Float,
    onPrev          : () -> Unit,
    onNext          : () -> Unit
) {
    val remaining    = totalBudget - totalSpent
    val headerColor  = progressColor(overallProgress)
    val hasBudgets   = totalBudget > BigDecimal.ZERO
    val isNextEnabled = selectedMonth < YearMonth.now()

    Surface(
        modifier       = Modifier.fillMaxWidth() .padding(vertical = 10.dp),
        color          = MaterialTheme.colorScheme.surface,
        shape          = MaterialTheme.shapes.extraSmall,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Month picker ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                MonthNavButton(onClick = onPrev, icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, desc = "Previous month")
                Text(
                    text          = selectedMonth.format(monthFormatter).uppercase(Locale.getDefault()),
                    style         = MaterialTheme.typography.titleLarge,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color         = MaterialTheme.colorScheme.primary
                )
                MonthNavButton(onClick = onNext, icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, desc = "Next month", enabled = isNextEnabled)
            }

            Spacer(Modifier.height(8.dp))

            // ── Spending summary ─────────────────────────────────────────
            if (hasBudgets) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text  = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = totalSpent.formatAsCurrency(),
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text  = "Total Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = totalBudget.formatAsCurrency(),
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress   = { overallProgress },
                    modifier   = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color      = headerColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text       = "${(overallProgress * 100).toInt()}% used",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = headerColor
                    )
                    Text(
                        text  = if (remaining >= BigDecimal.ZERO)
                            "${remaining.formatAsCurrency()} remaining"
                        else
                            "${remaining.abs().formatAsCurrency()} over budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remaining >= BigDecimal.ZERO)
                            MaterialTheme.colorScheme.onSurfaceVariant else BudgetOver,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text      = "Add category budgets to track your monthly spending",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MonthNavButton(onClick: () -> Unit, icon: ImageVector, desc: String, enabled: Boolean = true) {
    IconButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
    ) {
        Icon(
            icon, 
            contentDescription = desc, 
            tint = if (enabled) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Budget item card — accent stripe + status chip + cleaner amounts layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BudgetItemCard(
    budget        : BudgetUiModel,
    isDark        : Boolean,
    onEditClick   : () -> Unit,
    onDeleteClick : () -> Unit,
    modifier      : Modifier = Modifier
) {
    var showMenu          by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pColor   = progressColor(budget.progress)
    val status   = progressStatus(budget.progress)

    val animatedProgress by animateFloatAsState(
        targetValue   = budget.progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "budgetProgress"
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon  = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Budget") },
            text  = { Text("Remove the budget for ${budget.displayName}? Transactions won't be affected.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteClick(); showDeleteConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier  = modifier.fillMaxWidth().clickable { onEditClick() },
        shape     = MaterialTheme.shapes.large,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // Coloured left accent stripe — instant budget health readout
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(pColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp)
            ) {

                // ── Name · status chip · overflow menu ───────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = budget.displayName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Status chip
                    Surface(shape = CircleShape, color = pColor.copy(alpha = 0.12f)) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(status.icon, null, tint = pColor, modifier = Modifier.size(10.dp))
                            Text(
                                text       = status.label,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = pColor,
                                style      = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Overflow menu
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.MoreVert, "Options",
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text        = { Text("Edit") },
                                onClick     = { onEditClick(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text        = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick     = { showDeleteConfirm = true; showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Progress bar ─────────────────────────────────────────
                LinearProgressIndicator(
                    progress   = { animatedProgress },
                    modifier   = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color      = pColor,
                    trackColor = pColor.copy(alpha = 0.12f),
                    strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(Modifier.height(10.dp))

                // ── Amounts: SPENT · % · LEFT/OVER ──────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        AmountLabel("SPENT")
                        Text(
                            text       = budget.spent.formatAsCurrency(),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text       = "${(budget.progress * 100).toInt()}%",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = pColor
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        AmountLabel(if (budget.remaining >= BigDecimal.ZERO) "LEFT" else "OVER")
                        Text(
                            text       = budget.remaining.abs().formatAsCurrency(),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (budget.remaining >= BigDecimal.ZERO)
                                (if (isDark) DarkIncome else LightIncome)
                            else
                                (if (isDark) DarkExpense else LightExpense)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text      = "of ${budget.limit.formatAsCurrency()} limit",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AmountLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 9.sp,
        letterSpacing = 0.5.sp,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        style         = MaterialTheme.typography.labelSmall
    )
}

// Backward-compat alias for callers outside this file
@Composable
fun BudgetItem(budget: BudgetUiModel, onEditClick: () -> Unit, onDeleteClick: () -> Unit) =
    BudgetItemCard(budget, MaterialTheme.isDark, onEditClick, onDeleteClick)

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit bottom sheet — section labels, quick chips, contextual button
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheetContent(
    allCategories      : List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    budgetedCategories : List<String>,
    initialBudget      : BudgetUiModel? = null,
    onDismiss          : () -> Unit,
    onConfirm          : (String, Double) -> Unit
) {
    val isEditing = initialBudget != null

    var selectedCategory by remember {
        mutableStateOf(
            if (isEditing) allCategories.find { it.name == initialBudget!!.categoryName }
            else allCategories.firstOrNull { it.name !in budgetedCategories && it.type.uppercase() == "EXPENSE" }
        )
    }
    var limit by remember {
        mutableStateOf(
            if (isEditing) initialBudget!!.limit.stripTrailingZeros().toPlainString() else ""
        )
    }
    var expanded by remember { mutableStateOf(false) }

    val allBudgeted = !isEditing &&
            allCategories.none { it.name !in budgetedCategories && it.type.uppercase() == "EXPENSE" }
    val isValid = limit.toDoubleOrNull().let { it != null && it > 0 } && selectedCategory != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {

        // ── Sheet title ──────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text       = if (isEditing) "Edit Budget" else "New Monthly Budget",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = if (isEditing)
                        "Updating limit for ${initialBudget?.displayName}"
                    else
                        "Set a monthly spending cap for a category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        if (allBudgeted) {
            Surface(
                color    = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape    = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        text  = "All expense categories already have a budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {

            // ── Category ─────────────────────────────────────────────────
            SheetFieldLabel("CATEGORY")

            ExposedDropdownMenuBox(
                expanded         = expanded,
                onExpandedChange = { if (!isEditing) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value         = selectedCategory?.name ?: "Select a category",
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = {
                        if (!isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier      = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth(),
                    shape         = MaterialTheme.shapes.medium,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                if (!isEditing) {
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        allCategories
                            .filter { it.type.uppercase() == "EXPENSE" }
                            .forEach { category ->
                                val isBudgeted = category.name in budgetedCategories
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment     = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text  = category.name,
                                                color = if (isBudgeted)
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isBudgeted) {
                                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                                    Text(
                                                        text     = "Set",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style    = MaterialTheme.typography.labelSmall,
                                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = { if (!isBudgeted) { selectedCategory = category; expanded = false } },
                                    enabled = !isBudgeted
                                )
                            }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Monthly limit ─────────────────────────────────────────────
            SheetFieldLabel("MONTHLY LIMIT")

            OutlinedTextField(
                value         = limit,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { c -> c.isDigit() || c == '.' })
                        limit = input
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.fillMaxWidth(),
                prefix          = {
                    Text("₹ ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                },
                placeholder     = {
                    Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                shape           = MaterialTheme.shapes.medium,
                singleLine      = true,
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Quick amount chips
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1000" to "₹1K", "5000" to "₹5K", "10000" to "₹10K", "25000" to "₹25K")
                    .forEach { (raw, label) ->
                        FilterChip(
                            selected = limit == raw,
                            onClick  = { limit = raw },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Action button ────────────────────────────────────────────────
        Button(
            onClick  = {
                val l = limit.toDoubleOrNull() ?: 0.0
                if (l > 0 && selectedCategory != null) onConfirm(selectedCategory!!.name, l)
            },
            enabled  = isValid && !allBudgeted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape    = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text       = if (isEditing) "Update Budget" else "Save Budget",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SheetFieldLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier      = Modifier.padding(bottom = 6.dp)
    )
}