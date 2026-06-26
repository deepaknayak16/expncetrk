package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.core.common.formatToDisplay
import com.example.expncetracker.exptkr.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val list by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val advancedFilter by viewModel.advancedFilter.collectAsState()
    val smartFilters by viewModel.smartFilters.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showSplitDialog by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TransactionDetailContent(
                transaction = selectedTransaction!!,
                onEdit = { 
                    onNavigateToEdit(selectedTransaction!!.id)
                    selectedTransaction = null 
                },
                onDelete = { 
                    viewModel.deleteTransaction(selectedTransaction!!)
                    selectedTransaction = null 
                },
                onSplit = {
                    showSplitDialog = true
                },
                onSettle = {
                    viewModel.settleTransaction(selectedTransaction!!)
                    selectedTransaction = null
                }
            )
        }
    }

    if (showSplitDialog && selectedTransaction != null) {
        SplitTransactionDialog(
            transaction = selectedTransaction!!,
            categories = categories,
            onDismiss = { showSplitDialog = false; selectedTransaction = null },
            onConfirm = { splits ->
                viewModel.splitTransaction(selectedTransaction!!, splits)
                showSplitDialog = false
                selectedTransaction = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTransactions() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        placeholder = {
                            Text(
                                "Search transactions...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    MaterialTheme.shapes.extraLarge
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.title) },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    val isFilterActive = selectedFilter != DateFilter.MONTH
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (isFilterActive) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                                MaterialTheme.shapes.extraLarge
                            )
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (showFilterSheet) {
                    AdvancedFilterBottomSheet(
                        currentFilter = advancedFilter,
                        categories = categories,
                        onDismiss = { showFilterSheet = false },
                        onApply = { showFilterSheet = false },
                        onUpdate = { newFilter -> viewModel.updateAdvancedFilter { newFilter } },
                        onSaveSmartFilter = { viewModel.saveSmartFilter(it) }
                    )
                }

                if (smartFilters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartFilters.forEach { sf ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.applySmartFilter(sf) },
                                label = { Text(sf.name) },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (list.isEmpty()) {
                            item {
                                com.example.expncetracker.exptkr.ui.components.EmptyState(
                                    icon = Icons.Default.Search,
                                    title = if (searchQuery.isNotEmpty()) "No matches for \"$searchQuery\"" else "No transactions found",
                                    description = "Try adjusting your filters or search terms"
                                )
                            }
                        } else {
                            val grouped = list.sortedByDescending { it.timestamp }.groupBy { it.timestamp.year }
                            grouped.forEach { (year, yearTransactions) ->
                                if (grouped.size > 1) {
                                    item {
                                        Text(
                                            text = year.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                        )
                                    }
                                }

                                val dateGrouped = yearTransactions.groupBy {
                                    it.timestamp.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))
                                }

                                dateGrouped.forEach { (date, transactions) ->
                                    item {
                                        Text(
                                            text = date,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    
                                    items(transactions, key = { it.id }) { tx ->
                                        val categoryEntity = categories.find { it.name == tx.categoryName }
                                        TransactionListItem(
                                            transaction = tx,
                                            onClick = { selectedTransaction = tx },
                                            categoryIcon = categoryEntity?.let { com.example.expncetracker.exptkr.ui.components.getIconByName(it.iconName) },
                                            categoryColor = categoryEntity?.let { Color(it.color) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit,
    onSettle: () -> Unit
) {
    val isDark = MaterialTheme.isDark
    val canDelete = transaction.smsId == null && java.time.Duration.between(transaction.timestamp, java.time.LocalDateTime.now()).toMinutes() <= 60
    val canEdit = transaction.smsId == null && java.time.Duration.between(transaction.timestamp, java.time.LocalDateTime.now()).toMinutes() <= 60
    val canSplit = transaction.type == TransactionType.DEBIT
    val isDebt = transaction.type == TransactionType.LEND || transaction.type == TransactionType.BORROW

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Transaction Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = transaction.amount.formatAsCurrency(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = when(transaction.type) {
                TransactionType.CREDIT, TransactionType.BORROW -> if (isDark) DarkIncome else LightIncome
                TransactionType.DEBIT, TransactionType.LEND -> if (isDark) DarkExpense else LightExpense
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        Text(
            text = transaction.merchant,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailItemWithIcon(Icons.Default.Category, "Category", transaction.categoryName)
            if (transaction.counterparty != null) {
                DetailItemWithIcon(
                    Icons.Default.Person,
                    if (transaction.type == TransactionType.LEND) "Lent To" else "Borrowed From",
                    transaction.counterparty
                )
            }
            DetailItemWithIcon(Icons.AutoMirrored.Filled.Notes, "Note", transaction.note?.takeIf { it.isNotBlank() } ?: "No note")
            DetailItemWithIcon(Icons.Default.AccountBalanceWallet, "Account", transaction.bankName)
            DetailItemWithIcon(Icons.Default.AccessTime, "Date & Time", transaction.timestamp.formatToDisplay())
            DetailItemWithIcon(
                if (transaction.smsId != null) Icons.Default.Sms else Icons.Default.EditNote,
                "Source",
                if (transaction.smsId != null) "SMS Import" else "Manual Entry"
            )
        }

        Spacer(Modifier.height(24.dp))
        
        if (isDebt && !transaction.isSettled) {
            Button(
                onClick = onSettle,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark as Settled", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }

        if (canSplit) {
            Button(
                onClick = onSplit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Split Transaction", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
            
            Button(
                onClick = onEdit,
                enabled = canEdit,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit")
            }
        }
        
        if (transaction.smsId != null) {
            Surface(
                modifier = Modifier.padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "SMS imports have restricted editing for security and accuracy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailItemWithIcon(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTransactionDialog(
    transaction: Transaction,
    categories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, java.math.BigDecimal>>) -> Unit
) {
    var splitList by remember { 
        mutableStateOf(mutableListOf(
            transaction.categoryName to (transaction.amount.divide(java.math.BigDecimal(2), 2, java.math.RoundingMode.HALF_UP)),
            categories.firstOrNull { it.name != transaction.categoryName }?.name.orEmpty() to (transaction.amount.divide(java.math.BigDecimal(2), 2, java.math.RoundingMode.HALF_UP))
        )) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Transaction (Total: ₹${transaction.amount})") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                splitList.forEachIndexed { index, split ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = split.first,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = MaterialTheme.shapes.medium,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            val newList = splitList.toMutableList()
                                            newList[index] = cat.name to split.second
                                            splitList = newList
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = if (split.second.compareTo(java.math.BigDecimal.ZERO) == 0) "" else split.second.toString(),
                            onValueChange = { val amt = it.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                                val newList = splitList.toMutableList()
                                newList[index] = split.first to amt
                                splitList = newList
                            },
                            label = { Text("Amount") },
                            modifier = Modifier.weight(0.7f),
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                        )

                        if (splitList.size > 2) {
                            IconButton(onClick = { 
                                val newList = splitList.toMutableList()
                                newList.removeAt(index)
                                splitList = newList
                            }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = {
                        val newList = splitList.toMutableList()
                        newList.add("" to java.math.BigDecimal.ZERO)
                        splitList = newList
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Split")
                }

                val currentTotal = splitList.sumOf { it.second }
                val remaining = transaction.amount.subtract(currentTotal)
                Text(
                    text = "Total Split: ₹$currentTotal | Remaining: ₹$remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining.compareTo(java.math.BigDecimal.ZERO) == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(splitList) },
                enabled = (splitList.sumOf { it.second }.subtract(transaction.amount).abs()).compareTo(java.math.BigDecimal("0.01")) < 0 && splitList.all { it.first.isNotEmpty() }
            ) {
                Text("Confirm Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBottomSheet(
    currentFilter: TransactionFilter,
    categories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUpdate: (TransactionFilter) -> Unit,
    onSaveSmartFilter: (String) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = currentFilter.startDate,
        initialSelectedEndDateMillis = currentFilter.endDate
    )
    var showSmartFilterDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    onUpdate(TransactionFilter())
                }) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 1. Date Selection
            FilterSectionHeader("Period")
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                    val dateText = if (currentFilter.startDate != null && currentFilter.endDate != null) {
                        val start = java.time.Instant.ofEpochMilli(currentFilter.startDate)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val end = java.time.Instant.ofEpochMilli(currentFilter.endDate)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        "${start.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${end.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    } else "Select custom date range"
                    
                    Text(text = dateText, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Amount Range
            FilterSectionHeader("Amount Range")
            val min = 0f
            val max = 100000f
            var sliderPosition by remember { 
                mutableStateOf((currentFilter.minAmount?.toFloat() ?: min)..(currentFilter.maxAmount?.toFloat() ?: max)) 
            }
            RangeSlider(
                value = sliderPosition,
                onValueChange = { 
                    sliderPosition = it
                    onUpdate(currentFilter.copy(minAmount = it.start.toDouble().toBigDecimal(), maxAmount = it.endInclusive.toDouble().toBigDecimal()))
                },
                valueRange = min..max,
                steps = 100,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountBadge("Min: ₹${sliderPosition.start.roundToInt()}")
                AmountBadge("Max: ₹${sliderPosition.endInclusive.roundToInt()}")
            }

            Spacer(Modifier.height(24.dp))

            // 3. Category Selection
            FilterSectionHeader("Categories")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter.categoryName == null,
                    onClick = { onUpdate(currentFilter.copy(categoryName = null)) },
                    label = { Text("All Categories") },
                    leadingIcon = if (currentFilter.categoryName == null) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = currentFilter.categoryName == cat.name,
                        onClick = { onUpdate(currentFilter.copy(categoryName = cat.name)) },
                        label = { Text(cat.name) },
                        leadingIcon = if (currentFilter.categoryName == cat.name) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showSmartFilterDialog = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.StarOutline, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save View")
                }
                Button(
                    onClick = { onApply(); onDismiss() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Apply Filter")
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(currentFilter.copy(
                        startDate = dateRangePickerState.selectedStartDateMillis,
                        endDate = dateRangePickerState.selectedEndDateMillis
                    ))
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = null,
                headline = null,
                showModeToggle = false
            )
        }
    }

    if (showSmartFilterDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSmartFilterDialog = false },
            title = { Text("Save Filter View") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("View Name") },
                    placeholder = { Text("e.g. Shopping last month") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                Button(onClick = { 
                    if (name.isNotBlank()) {
                        onSaveSmartFilter(name)
                        showSmartFilterDialog = false 
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartFilterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun AmountBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = CircleShape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}


