package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.components.getIconByName
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.core.common.formatToDisplay
import com.example.expncetracker.exptkr.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val list by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val domainCategories = remember(categories) {
        categories.map { entity ->
            Category(
                id = entity.name,
                name = entity.name,
                type = entity.type,
                icon = entity.iconName,
                color = String.format("#%06X", (0xFFFFFF and entity.color))
            )
        }
    }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val advancedFilter by viewModel.advancedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showFilterSheet) {
        AdvancedLedgerFilterBottomSheet(
            initialFilter = advancedFilter,
            categories = categories,
            onDismissRequest = { showFilterSheet = false },
            onApplyFilters = { newFilter ->
                viewModel.updateAdvancedFilter { newFilter }
            }
        )
    }

    if (selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
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
                    // Split feature temporarily disabled to fix build
                    selectedTransaction = null
                },
                onSettle = {
                    viewModel.settleTransaction(selectedTransaction!!)
                    selectedTransaction = null
                }
            )
        }
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
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        BadgedBox(
                            badge = {
                                if (advancedFilter != TransactionFilter()) {
                                    Badge(modifier = Modifier.size(8.dp))
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading && list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No transactions found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(list, key = { it.id }) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                categories = domainCategories,
                                onClick = { selectedTransaction = transaction }
                            )
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
    val isOthers = transaction.categoryName.trim().lowercase().let { it == "other" || it == "others" }
    val isSms = transaction.smsId != null
    
    // STRICT RULES: 
    // 1. Delete is always disabled
    // 2. Edit is NOT allowed if the system already detected a category (SMS + not Other)
    // 3. Edit is only allowed once for Manual or "Other" SMS
    val canDelete = false
    val canEdit = when {
        isSms && !isOthers -> false // System detected category: Lock immediately
        transaction.isCategoryManuallyCorrected -> false // Already corrected once: Lock
        else -> true // Manual or "Other" SMS: Allow one correction
    }
    
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
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
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
            DetailItemWithIcon(
                icon = Icons.Default.Category,
                label = "Category",
                value = transaction.categoryName
            )
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
                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Split Transaction", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
        
        if (!canEdit) {
            Surface(
                modifier = Modifier.padding(bottom = 12.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                val lockReason = if (isSms && !isOthers) 
                    "This transaction was automatically categorized and is verified." 
                    else "This transaction has already been edited once and is now locked."
                
                Text(
                    text = lockReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
                color = if (isOthers) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOthers) Icons.Default.PriorityHigh else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isOthers) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            isOthers && canEdit -> "This transaction is uncategorized. Tap Edit to classify it."
                            isOthers && !canEdit -> "This transaction is uncategorized and locked."
                            else -> "Bank data is locked for security and accuracy."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOthers) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
