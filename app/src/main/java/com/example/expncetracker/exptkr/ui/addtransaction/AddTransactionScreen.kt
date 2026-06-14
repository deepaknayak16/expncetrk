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
import androidx.compose.ui.graphics.graphicsLayer
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

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val suggestedCategory by viewModel.suggestedCategory.collectAsState()

    LaunchedEffect(suggestedCategory) {
        if (merchantName.isNotEmpty() && (selectedCategoryName.isEmpty() || selectedCategoryName == "Others")) {
            suggestedCategory?.let { selectedCategoryName = it }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0L) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(transactionToEdit) {
        transactionToEdit?.let {
            amountText = if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else "%.2f".format(it.amount)
            merchantName = it.merchant
            note = it.note ?: ""
            selectedType = it.type
            selectedCategoryName = it.categoryName
            transactionDate = it.timestamp
            counterparty = it.counterparty ?: ""
            isRecurring = it.isRecurring
            it.frequency?.let { freq -> recurrenceFrequency = freq }
        }
    }

    LaunchedEffect(allCategories, selectedType) {
        if (selectedCategoryName.isEmpty() && allCategories.isNotEmpty()) {
            val typeStr = when (selectedType) {
                TransactionType.CREDIT -> "INCOME"
                TransactionType.DEBIT -> "EXPENSE"
                else -> "EXPENSE"
            }
            selectedCategoryName = allCategories.find { it.type == typeStr }?.name ?: allCategories.first().name
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
                title = { 
                    Text(
                        if (transactionId != null) "Edit Entry" else "New Entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (amountText.isNotEmpty() && !amountText.last().isDigit() && amountText.last() != ')') {
                                Toast.makeText(context, "Incomplete expression", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            
                            val amount = try { evaluate(amountText) } catch (e: Exception) { -1.0 }
                            when {
                                amount < 0 -> Toast.makeText(context, "Invalid expression", Toast.LENGTH_SHORT).show()
                                amount == 0.0 -> Toast.makeText(context, "Enter amount", Toast.LENGTH_SHORT).show()
                                merchantName.isBlank() -> Toast.makeText(context, "Enter merchant", Toast.LENGTH_SHORT).show()
                                selectedAccount == null -> Toast.makeText(context, "Select account", Toast.LENGTH_SHORT).show()
                                else -> {
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
                                        timestamp = transactionDate
                                    )
                                    onNavigateBack()
                                }
                            }
                        }
                    ) {
                        Text("SAVE", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            ) {
                // 1. Transaction Type Toggle (Compact)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val types = listOf(
                            TransactionType.DEBIT to "EXPENSE",
                            TransactionType.CREDIT to "INCOME",
                            TransactionType.TRANSFER to "TRANSFER",
                            TransactionType.LEND to "LEND",
                            TransactionType.BORROW to "BORROW"
                        )
                        types.forEach { (type, label) ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        selectedType = type
                                        val typeStr = when (type) {
                                            TransactionType.CREDIT -> "INCOME"
                                            TransactionType.DEBIT -> "EXPENSE"
                                            else -> "EXPENSE"
                                        }
                                        val currentCatValid = allCategories.find { it.name == selectedCategoryName }?.type == typeStr
                                        if (!currentCatValid) {
                                            allCategories.find { it.type == typeStr }?.let { selectedCategoryName = it.name }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2. Amount Display (Dense)
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("₹", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = formattedAmount,
                            style = if (formattedAmount.length > 12) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (amountText.length > 1) amountText = amountText.dropLast(1) else amountText = "0"
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 3. Merchant Field
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(8.dp))

                // 4. Account & Category (Row)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactSelectorDense(
                        label = "From Account",
                        value = selectedAccount?.name ?: "Select",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }

                    CompactSelectorDense(
                        label = "Category",
                        value = selectedCategoryName.ifEmpty { "Select" },
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f)
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(8.dp))

                // 5. Date & Time (Row)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(transactionDate.format(dateFormatter), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(transactionDate.format(timeFormatter), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 6. Person field (Optional, only if Lend/Borrow)
                if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        placeholder = { Text("Person / Contact", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 7. Note field
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

                // 8. Recurring Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Repeat monthly?", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isRecurring, 
                        onCheckedChange = { isRecurring = it }, 
                        modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)
                    )
                }
            }

            // 9. Keypad (Pinned to Bottom, Highly Compact)
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                CalculatorKeypadDense(
                    modifier = Modifier.height(230.dp),
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText == "0" && digit != "(" && digit != ")") {
                            amountText = digit 
                        } else {
                            if (digit == "(" && amountText.isNotEmpty() && (amountText.last().isDigit() || amountText.last() == ')')) {
                                amountText += "*("
                            } else if (digit.first().isDigit() && amountText.isNotEmpty() && amountText.last() == ')') {
                                amountText += "*$digit"
                            } else {
                                if (amountText == "0") amountText = digit
                                else if (amountText.length < 24) amountText += digit
                            }
                        }
                    },
                    onOperatorClick = { op ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText.isNotEmpty()) {
                            val lastChar = amountText.last()
                            if (lastChar in "+-*/") {
                                amountText = amountText.dropLast(1) + op
                            } else if (lastChar == '(') {
                                if (op == "-") amountText += op
                            } else if (lastChar != '.') {
                                amountText += op
                            }
                        } else if (op == "-") {
                            amountText = "-"
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
                        if (amountText.isEmpty() || amountText == "0") return@CalculatorKeypadDense
                        
                        if (!amountText.last().isDigit() && amountText.last() != ')') {
                            Toast.makeText(context, "Incomplete expression", Toast.LENGTH_SHORT).show()
                            return@CalculatorKeypadDense
                        }

                        val openBrackets = amountText.count { it == '(' }
                        val closeBrackets = amountText.count { it == ')' }
                        if (openBrackets != closeBrackets) {
                            Toast.makeText(context, "Unbalanced brackets", Toast.LENGTH_SHORT).show()
                            return@CalculatorKeypadDense
                        }

                        try {
                            val result = evaluate(amountText)
                            amountText = if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(result)
                        } catch (e: Exception) { 
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Modal Sheets and Dialogs
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
                TransactionType.TRANSFER -> allCategories.filter { it.type == "EXPENSE" || it.name == "Others" }
                else -> allCategories
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesToShow) { category ->
                        val color = Color(category.color)
                        val icon = getIconByName(category.iconName)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { 
                                selectedCategoryName = category.name
                                showCategorySheet = false 
                            }.padding(4.dp)
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(text = category.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
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
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                            selectedAccount = account
                            showAccountSheet = false
                        }.padding(vertical = 12.dp, horizontal = 8.dp),
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
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CompactSelectorDense(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(42.dp).clickable { onClick() },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun CalculatorKeypadDense(
    modifier: Modifier = Modifier,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onEqualsClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        val rowModifier = Modifier.fillMaxWidth().weight(1f)
        Row(modifier = rowModifier) {
            KeyButtonDense("7", modifier = Modifier.weight(1f)) { onDigitClick("7") }
            KeyButtonDense("8", modifier = Modifier.weight(1f)) { onDigitClick("8") }
            KeyButtonDense("9", modifier = Modifier.weight(1f)) { onDigitClick("9") }
            KeyButtonDense("÷", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("/") }
        }
        Row(modifier = rowModifier) {
            KeyButtonDense("4", modifier = Modifier.weight(1f)) { onDigitClick("4") }
            KeyButtonDense("5", modifier = Modifier.weight(1f)) { onDigitClick("5") }
            KeyButtonDense("6", modifier = Modifier.weight(1f)) { onDigitClick("6") }
            KeyButtonDense("×", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("*") }
        }
        Row(modifier = rowModifier) {
            KeyButtonDense("1", modifier = Modifier.weight(1f)) { onDigitClick("1") }
            KeyButtonDense("2", modifier = Modifier.weight(1f)) { onDigitClick("2") }
            KeyButtonDense("3", modifier = Modifier.weight(1f)) { onDigitClick("3") }
            KeyButtonDense("(", modifier = Modifier.weight(1f), isOperator = true) { onDigitClick("(") }
        }
        Row(modifier = rowModifier) {
            KeyButtonDense(")", modifier = Modifier.weight(1f), isOperator = true) { onDigitClick(")") }
            KeyButtonDense("0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButtonDense("+", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("+") }
            KeyButtonDense("-", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("-") }
        }
        Row(modifier = rowModifier) {
            KeyButtonDense(".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButtonDense("=", modifier = Modifier.weight(3f), isAction = true) { onEqualsClick() }
        }
    }
}

@Composable
fun RowScope.KeyButtonDense(
    text: String,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAction -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier.fillMaxHeight().background(backgroundColor).border(0.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.titleMedium, 
            color = if (isAction) MaterialTheme.colorScheme.onPrimary else if (isOperator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
            fontWeight = if (isOperator || isAction) FontWeight.Bold else FontWeight.Normal
        )
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
