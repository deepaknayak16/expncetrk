package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import com.example.expncetracker.exptkr.ui.dashboard.TimeFilterRow
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.core.common.formatToDisplay
import com.example.expncetracker.exptkr.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val list by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = {
                            Text(
                                "Search...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                                            Icon(Icons.Default.Check, contentDescription = null)
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
                            .size(56.dp)
                            .background(
                                if (isFilterActive) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surface, 
                                MaterialTheme.shapes.large
                            )
                            .border(
                                1.dp, 
                                if (isFilterActive) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outlineVariant, 
                                MaterialTheme.shapes.large
                            )
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showFilterSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showFilterSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Box(Modifier.padding(16.dp)) {
                            TimeFilterRow(
                                currentFilter = selectedFilter,
                                onFilterSelected = { 
                                    viewModel.setFilter(it)
                                    showFilterSheet = false 
                                }
                            )
                        }
                        Spacer(Modifier.height(24.dp))
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
                                            style = MaterialTheme.typography.titleSmall,
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
    val canEdit = transaction.smsId == null
    val canSplit = transaction.type == TransactionType.DEBIT
    val isDebt = transaction.type == TransactionType.LEND || transaction.type == TransactionType.BORROW

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Transaction Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        
        Surface(modifier = Modifier.size(80.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = transaction.amount.formatAsCurrency(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = when(transaction.type) {
                TransactionType.CREDIT, TransactionType.BORROW -> if (isDark) DarkIncome else LightIncome
                TransactionType.DEBIT, TransactionType.LEND -> if (isDark) DarkExpense else LightExpense
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        DetailItem("Merchant", transaction.merchant)
        DetailItem("Category", transaction.categoryName)
        if (transaction.counterparty != null) {
            DetailItem(if (transaction.type == TransactionType.LEND) "Lent To" else "Borrowed From", transaction.counterparty!!)
        }
        DetailItem("Note", transaction.note?.takeIf { it.isNotBlank() } ?: "No note")
        DetailItem("Bank/Account", transaction.bankName)
        DetailItem("Date & Time", transaction.timestamp.formatToDisplay())
        DetailItem("Source", if (transaction.smsId != null) "SMS Import" else "Manual Entry")
        DetailItem("Type", transaction.type.name)
        
        if (transaction.isSettled) {
            DetailItem("Status", "SETTLED")
        }

        Spacer(Modifier.height(24.dp))
        
        if (isDebt && !transaction.isSettled) {
            Button(
                onClick = onSettle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mark as Settled")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onSplit,
            enabled = canSplit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.CallSplit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Split Transaction")
        }

        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
            
            Button(onClick = onEdit, enabled = canEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Edit")
            }
        }
        
        if (transaction.smsId != null) {
            Text("SMS transactions are immutable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTransactionDialog(
    transaction: Transaction,
    categories: List<com.example.expncetracker.exptkr.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, Double>>) -> Unit
) {
    var splitList by remember { 
        mutableStateOf(mutableListOf(
            transaction.categoryName to (transaction.amount / 2),
            categories.firstOrNull { it.name != transaction.categoryName }?.name.orEmpty() to (transaction.amount / 2)
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
                            value = if (split.second == 0.0) "" else split.second.toString(),
                            onValueChange = { val amt = it.toDoubleOrNull() ?: 0.0
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
                        newList.add("" to 0.0)
                        splitList = newList
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Split")
                }

                val currentTotal = splitList.sumOf { it.second }
                val remaining = transaction.amount - currentTotal
                Text(
                    text = "Total Split: ₹$currentTotal | Remaining: ₹$remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(splitList) },
                enabled = kotlin.math.abs(splitList.sumOf { it.second } - transaction.amount) < 0.01 && splitList.all { it.first.isNotEmpty() }
            ) {
                Text("Confirm Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            value, 
            color = MaterialTheme.colorScheme.onSurface, 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
