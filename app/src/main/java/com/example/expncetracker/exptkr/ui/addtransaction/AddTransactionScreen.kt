package com.example.expncetracker.exptkr.ui.addtransaction

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import com.example.expncetracker.exptkr.ui.theme.*
import com.example.expncetracker.exptkr.ui.components.getCategoryIcon
import com.example.expncetracker.exptkr.ui.components.getIconByName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val transactionToEdit by viewModel.transactionToEdit.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val suggestedCategory by viewModel.suggestedCategory.collectAsState()

    var amountText by remember { mutableStateOf("0") }
    var merchantName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategoryName by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<AccountUiModel?>(null) }
    var transactionDate by remember { mutableStateOf(LocalDateTime.now()) }
    var counterparty by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var tagsInput by remember { mutableStateOf("") }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0L) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(transactionToEdit) {
        transactionToEdit?.let {
            amountText = if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else "%.2f".format(it.amount)
            merchantName = it.merchant
            note = it.note ?: ""
            selectedType = it.type
            selectedCategoryName = it.categoryName
            transactionDate = it.timestamp
            counterparty = it.counterparty ?: ""
            isRecurring = it.isRecurring
            it.frequency?.let { freq -> recurrenceFrequency = freq }
            tagsInput = it.tags.joinToString(", ")
        }
    }

    LaunchedEffect(suggestedCategory) {
        suggestedCategory?.let {
            if (selectedCategoryName.isEmpty() || selectedCategoryName == "Others") {
                selectedCategoryName = it
            }
        }
    }

    LaunchedEffect(allCategories) {
        if (selectedCategoryName.isEmpty() && allCategories.isNotEmpty()) {
            selectedCategoryName = allCategories.find { it.type == "EXPENSE" }?.name ?: ""
        }
    }

    LaunchedEffect(accounts, transactionToEdit) {
        if (transactionToEdit != null) {
            selectedAccount = accounts.find { acc -> acc.name == transactionToEdit?.bankName }
        } else if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.first()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transactionDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = transactionDate.hour,
        initialMinute = transactionDate.minute
    )

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }

    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (transactionId != null) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val amount = try { evaluate(amountText) } catch (e: Exception) { -1.0 }
                            when {
                                amount < 0 -> Toast.makeText(context, "Invalid expression", Toast.LENGTH_SHORT).show()
                                amount == 0.0 -> Toast.makeText(context, "Enter amount", Toast.LENGTH_SHORT).show()
                                merchantName.isBlank() -> Toast.makeText(context, "Enter merchant", Toast.LENGTH_SHORT).show()
                                selectedAccount == null -> Toast.makeText(context, "Select account", Toast.LENGTH_SHORT).show()
                                else -> {
                                    val tagList = tagsInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .map { if (it.startsWith("#")) it else "#$it" }

                                    viewModel.addTransaction(
                                        id = transactionId ?: 0L,
                                        amount = amount,
                                        type = selectedType,
                                        category = selectedCategoryName,
                                        description = merchantName.trim(),
                                        note = note.trim(),
                                        bankName = selectedAccount!!.name,
                                        counterparty = counterparty.trim().ifEmpty { null },
                                        isRecurring = isRecurring,
                                        frequency = if (isRecurring) recurrenceFrequency else null,
                                        tags = tagList,
                                        timestamp = transactionDate
                                    )
                                    onNavigateBack()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Transaction Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val types = listOf(
                        TransactionType.CREDIT to "INCOME",
                        TransactionType.DEBIT to "EXPENSE",
                        TransactionType.TRANSFER to "TRANSFER",
                        TransactionType.LEND to "LEND",
                        TransactionType.BORROW to "BORROW"
                    )
                    
                    types.forEachIndexed { index, (type, label) ->
                        TypeTab(label, selectedType == type) { 
                            selectedType = type
                            if (type == TransactionType.CREDIT) {
                                allCategories.find { it.type == "INCOME" }?.let { selectedCategoryName = it.name }
                            } else if (type == TransactionType.DEBIT) {
                                allCategories.find { it.type == "EXPENSE" }?.let { selectedCategoryName = it.name }
                            }
                        }
                        if (index < types.size - 1) {
                            Text("|", color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactSelector(
                        value = selectedAccount?.name ?: "Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }
                    
                    CompactSelector(
                        value = selectedCategoryName.ifEmpty { "Category" },
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f)
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(12.dp))

                // Amount Display
                val formattedAmount = remember(amountText) {
                    if (amountText.isEmpty()) "0"
                    else if (amountText.any { it in "+-*/()" }) amountText
                    else {
                        try {
                            val parts = amountText.split(".")
                            val intPart = parts[0].toLongOrNull() ?: 0L
                            val formattedInt = java.text.NumberFormat.getNumberInstance(Locale.US).format(intPart)
                            if (parts.size > 1) "$formattedInt.${parts[1]}" else formattedInt
                        } catch (e: Exception) {
                            amountText
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("₹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = formattedAmount,
                            style = if (formattedAmount.length > 10) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (amountText.length > 1) amountText = amountText.dropLast(1) else amountText = "0"
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        placeholder = { Text(if (selectedType == TransactionType.LEND) "Lent to whom?" else "Borrowed from whom?", style = MaterialTheme.typography.bodyMedium) },
                        label = { Text("Person / Contact") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { 
                        merchantName = it 
                        viewModel.onMerchantNameChanged(it, selectedCategoryName)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Merchant / Payee", style = MaterialTheme.typography.bodyMedium) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Note (Optional)", style = MaterialTheme.typography.bodyMedium) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Tags (e.g. business, trip)", style = MaterialTheme.typography.bodyMedium) },
                    label = { Text("Labels / Tags") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recurring Transaction", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }

                if (isRecurring) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.entries.forEach { freq ->
                            val isSelected = recurrenceFrequency == freq
                            FilterChip(
                                selected = isSelected,
                                onClick = { recurrenceFrequency = freq },
                                label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }

            CalculatorKeypad(
                modifier = Modifier.height(280.dp),
                onDigitClick = { digit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (amountText == "0" && digit != "(" && digit != ")") amountText = digit 
                    else if (amountText == "0") amountText = digit
                    else if (amountText.length < 24) amountText += digit
                },
                onOperatorClick = { op ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (amountText.isNotEmpty() && !amountText.last().isDigit() && amountText.last() != '.' && amountText.last() != ')' && amountText.last() != '(') {
                        amountText = amountText.dropLast(1) + op
                    } else if (amountText.isNotEmpty() && amountText.last() != '.') {
                        amountText += op
                    }
                },
                onDecimalClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val lastPart = amountText.split('+', '-', '*', '/', '(', ')').last()
                    if (!lastPart.contains(".")) {
                        amountText += if (amountText.isEmpty() || !amountText.last().isDigit()) "0." else "."
                    }
                },
                onEqualsClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        val result = evaluate(amountText)
                        amountText = if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(result)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid expression", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDatePicker = true }, contentPadding = PaddingValues(4.dp)) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(transactionDate.format(dateFormatter), style = MaterialTheme.typography.labelLarge)
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    TextButton(onClick = { showTimePicker = true }, contentPadding = PaddingValues(4.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(transactionDate.format(timeFormatter), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    // Modal Sheets and Dialogs (unchanged)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        transactionDate = LocalDateTime.of(newDate, transactionDate.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    transactionDate = LocalDateTime.of(
                        transactionDate.toLocalDate(),
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val categoriesToShow = when (selectedType) {
                TransactionType.CREDIT -> allCategories.filter { it.type == "INCOME" }
                TransactionType.DEBIT -> allCategories.filter { it.type == "EXPENSE" }
                else -> allCategories
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoriesToShow) { category ->
                    val color = Color(category.color)
                    val icon = getIconByName(category.iconName)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.height(90.dp).clip(MaterialTheme.shapes.medium).clickable { 
                            selectedCategoryName = category.name
                            showCategorySheet = false 
                        }.padding(4.dp)
                    ) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = category.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Select Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                if (accounts.isEmpty()) {
                    Text("No accounts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedAccount = account
                            showAccountSheet = false
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = account.color.copy(alpha = 0.2f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = account.color, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TypeTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.clickable { onClick() }.padding(vertical = 4.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
    )
}

@Composable
fun CompactSelector(
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(48.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onEqualsClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val rowModifier = Modifier.fillMaxWidth().weight(1f)
        Row(modifier = rowModifier) {
            KeyButton("7", modifier = Modifier.weight(1f)) { onDigitClick("7") }
            KeyButton("8", modifier = Modifier.weight(1f)) { onDigitClick("8") }
            KeyButton("9", modifier = Modifier.weight(1f)) { onDigitClick("9") }
            KeyButton("÷", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("/") }
        }
        Row(modifier = rowModifier) {
            KeyButton("4", modifier = Modifier.weight(1f)) { onDigitClick("4") }
            KeyButton("5", modifier = Modifier.weight(1f)) { onDigitClick("5") }
            KeyButton("6", modifier = Modifier.weight(1f)) { onDigitClick("6") }
            KeyButton("×", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("*") }
        }
        Row(modifier = rowModifier) {
            KeyButton("1", modifier = Modifier.weight(1f)) { onDigitClick("1") }
            KeyButton("2", modifier = Modifier.weight(1f)) { onDigitClick("2") }
            KeyButton("3", modifier = Modifier.weight(1f)) { onDigitClick("3") }
            KeyButton("(", modifier = Modifier.weight(1f), isOperator = true) { onDigitClick("(") }
        }
        Row(modifier = rowModifier) {
            KeyButton(")", modifier = Modifier.weight(1f), isOperator = true) { onDigitClick(")") }
            KeyButton("0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButton("+", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("+") }
            KeyButton("-", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("-") }
        }
        Row(modifier = rowModifier) {
            KeyButton(".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButton("=", modifier = Modifier.weight(3f), isAction = true) { onEqualsClick() }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAction -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier.fillMaxHeight().background(backgroundColor).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = if (isAction) MaterialTheme.colorScheme.onPrimary else if (isOperator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isOperator || isAction) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun evaluate(expression: String): Double {
    val cleanExpr = expression.trim().replace("÷", "/").replace("×", "*")
    if (cleanExpr.isEmpty()) return 0.0
    
    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < cleanExpr.length) cleanExpr[pos].toInt() else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.toInt()) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < cleanExpr.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.toInt())) x += parseTerm()
                else if (eat('-'.toInt())) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.toInt())) x *= parseFactor()
                else if (eat('/'.toInt())) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Div by zero")
                    x /= divisor
                } else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.toInt())) return parseFactor()
            if (eat('-'.toInt())) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.toInt())) {
                x = parseExpression()
                eat(')'.toInt())
            } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                x = cleanExpr.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }
            return x
        }
    }.parse()
}
