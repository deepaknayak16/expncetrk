package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedLedgerFilterBottomSheet(
    initialFilter: TransactionFilter,
    categories: List<CategoryEntity>,
    onDismissRequest: () -> Unit,
    onApplyFilters: (TransactionFilter) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // State Tracking
    var startDate by remember { mutableStateOf(initialFilter.startDate) }
    var endDate by remember { mutableStateOf(initialFilter.endDate) }
    var minAmount by remember { mutableStateOf(initialFilter.minAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(initialFilter.maxAmount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(initialFilter.type) }
    var selectedStatus by remember { mutableStateOf(initialFilter.isSettled) }
    var selectedCategory by remember { mutableStateOf(initialFilter.categoryName) }

    val transactionTypes = TransactionType.entries
    val locale = remember { Locale.getDefault() }
    
    // Date Picker state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatDate(millis: Long?): String {
        return millis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        } ?: "Select Date"
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate ?: Instant.now().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: Instant.now().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() 
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Search", 
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    startDate = null; endDate = null; minAmount = ""; maxAmount = ""; selectedType = null; selectedStatus = null; selectedCategory = null
                }) {
                    Text(text = "Reset All", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Date Range
            Text("Date Range", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formatDate(startDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("From") },
                    trailingIcon = { 
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formatDate(endDate),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("To") },
                    trailingIcon = { 
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Amount Range
            Text("Amount Range", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { minAmount = it },
                    label = { Text("Min Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { maxAmount = it },
                    label = { Text("Max Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Transaction Type
            Text("Transaction Type", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transactionTypes.forEach { type ->
                    val label = when(type) {
                        TransactionType.DEBIT -> "Expense"
                        TransactionType.CREDIT -> "Income"
                        else -> type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { 
                            val newType = if (selectedType == type) null else type
                            selectedType = newType
                            
                            // Reset related filters when type changes
                            if (newType != TransactionType.DEBIT && newType != TransactionType.CREDIT) {
                                selectedCategory = null
                            }
                            if (newType != TransactionType.LEND && newType != TransactionType.BORROW) {
                                selectedStatus = null
                            }
                        },
                        label = { Text(label) },
                        leadingIcon = if (selectedType == type) {
                            { Icon(Icons.Default.Done, modifier = Modifier.size(FilterChipDefaults.IconSize), contentDescription = null) }
                        } else null
                    )
                }
            }

            // Category Selection (Horizontal Scroll)
            if (selectedType == TransactionType.DEBIT || selectedType == TransactionType.CREDIT) {
                val typeString = if (selectedType == TransactionType.DEBIT) "EXPENSE" else "INCOME"
                val filteredCategories = categories.filter { it.type.uppercase() == typeString }
                
                if (filteredCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategories) { category ->
                            FilterChip(
                                selected = selectedCategory == category.name,
                                onClick = { selectedCategory = if (selectedCategory == category.name) null else category.name },
                                label = { Text(category.name) },
                                leadingIcon = if (selectedCategory == category.name) {
                                    { Icon(Icons.Default.Done, modifier = Modifier.size(FilterChipDefaults.IconSize), contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Settlement Status
            if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                Text("Settlement Status", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(true to "Settled", false to "Outstanding").forEach { (status, label) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = if (selectedStatus == status) null else status },
                            label = { Text(label) },
                            leadingIcon = if (selectedStatus == status) {
                                { Icon(Icons.Default.Done, modifier = Modifier.size(FilterChipDefaults.IconSize), contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // CTA Button
            Button(
                onClick = {
                    onApplyFilters(
                        initialFilter.copy(
                            startDate = startDate,
                            endDate = endDate,
                            minAmount = minAmount.toBigDecimalOrNull(),
                            maxAmount = maxAmount.toBigDecimalOrNull(),
                            type = selectedType,
                            isSettled = selectedStatus,
                            categoryName = selectedCategory
                        )
                    )
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Apply Search Criteria", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
