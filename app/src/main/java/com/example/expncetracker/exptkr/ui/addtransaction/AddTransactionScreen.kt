package com.example.expncetracker.exptkr.ui.addtransaction

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import com.example.expncetracker.exptkr.ui.components.getIconByName
import java.time.Instant
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

    var amountText by remember { mutableStateOf("") }
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
    var userSelectedCategory by remember { mutableStateOf(false) }

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) counterparty = c.getString(nameIndex)
                }
            }
        }
    }

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
            if (!userSelectedCategory && (selectedCategoryName.isEmpty() || selectedCategoryName == "Others")) {
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
    var showCategorySheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transactionDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = transactionDate.hour,
        initialMinute = transactionDate.minute
    )

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (transactionId != null) "Edit Transaction" else "Add Transaction",
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
            // Main Content Area (Form Fields)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Transaction Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val types = listOf(
                        TransactionType.CREDIT to "Income",
                        TransactionType.DEBIT to "Expense",
                        TransactionType.TRANSFER to "Transfer",
                        TransactionType.LEND to "Lend",
                        TransactionType.BORROW to "Borrow"
                    )
                    
                    types.forEach { (type, label) ->
                        TypeTab(label, selectedType == type) { 
                            selectedType = type
                            if (type == TransactionType.CREDIT) {
                                allCategories.find { it.type == "INCOME" }?.let { selectedCategoryName = it.name }
                            } else if (type == TransactionType.DEBIT) {
                                allCategories.find { it.type == "EXPENSE" }?.let { selectedCategoryName = it.name }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernSelector(
                        value = selectedAccount?.name ?: "Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }
                    
                    ModernSelector(
                        value = selectedCategoryName.ifEmpty { "Category" },
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f)
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(8.dp))

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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("₹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Form Fields (Optimized Height)
                val fieldModifier = Modifier.fillMaxWidth().height(56.dp)
                
                if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        modifier = fieldModifier,
                        placeholder = { Text(if (selectedType == TransactionType.LEND) "Lent to?" else "Borrowed from?", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            IconButton(
                                onClick = { contactPicker.launch(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContactPage, contentDescription = "Pick contact", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    Spacer(Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { 
                        merchantName = it 
                        viewModel.onMerchantNameChanged(it, selectedCategoryName)
                    },
                    modifier = fieldModifier,
                    placeholder = { Text("Merchant / Payee", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = fieldModifier,
                    placeholder = { Text("Note (Optional)", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    modifier = fieldModifier,
                    placeholder = { Text("Labels / Tags", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Recurring Switch
                Row(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recurring Transaction", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isRecurring, 
                        onCheckedChange = { isRecurring = it },
                        modifier = Modifier.scale(0.75f)
                    )
                }

                if (isRecurring) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RecurrenceFrequency.entries.forEach { freq ->
                            val isSelected = recurrenceFrequency == freq
                            FilterChip(
                                selected = isSelected,
                                onClick = { recurrenceFrequency = freq },
                                label = { Text(freq.name.take(3), style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Fixed Section (Calculator Integrated with Date/Time)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117)) // Deep Dark
                    .padding(top = 4.dp)
            ) {
                // Calculator (Slightly more compact)
                CalculatorKeypad(
                    modifier = Modifier.height(260.dp).padding(horizontal = 8.dp),
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText == "0") amountText = digit 
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
                    },
                    onClearClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        amountText = ""
                    },
                    onDeleteClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
                    },
                    onBracketClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val openBrackets = amountText.count { it == '(' }
                        val closeBrackets = amountText.count { it == ')' }
                        
                        if (amountText.isEmpty()) {
                            amountText = "("
                        } else {
                            val lastChar = if (amountText.isNotEmpty()) amountText.last() else ' '
                            if (lastChar.isDigit() || lastChar == ')') {
                                if (openBrackets > closeBrackets) amountText += ")"
                                else amountText += "*("
                            } else {
                                amountText += "("
                            }
                        }
                    }
                )

                // Date/Time Row (Integrated at the absolute bottom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(transactionDate.format(dateFormatter), style = MaterialTheme.typography.labelMedium)
                    }
                    
                    Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.White.copy(alpha = 0.2f)))
                    
                    TextButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(transactionDate.format(timeFormatter), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    // Modal Sheets and Dialogs (remains functional)
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
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            transactionDate = LocalDateTime.of(
                                transactionDate.toLocalDate(),
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            )
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
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
                TransactionType.TRANSFER -> allCategories.filter { it.name == "Transfer" || it.name == "Others" }
                else -> allCategories
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
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
                            userSelectedCategory = true
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
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
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
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(account.color).copy(alpha = 0.2f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(account.color), modifier = Modifier.size(20.dp))
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
    Surface(
        onClick = onClick,
        modifier = Modifier.clip(CapsuleShape),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private val CapsuleShape = RoundedCornerShape(16.dp)

@Composable
fun ModernSelector(
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(48.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBracketClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1117))
            .padding(8.dp)
    ) {
        val rowModifier = Modifier.fillMaxWidth().weight(1f)
        val opColor = Color(0xFF1E2631)
        val accentColor = Color(0xFF3B82F6)
        val clearColor = Color(0xFFEF4444).copy(alpha = 0.2f)
        val equalsColor = Color(0xFF10B981)

        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(text = "AC", modifier = Modifier.weight(1f), backgroundColor = clearColor, contentColor = Color(0xFFEF4444)) { onClearClick() }
            KeyButton(text = "( )", modifier = Modifier.weight(1f), backgroundColor = opColor) { onBracketClick() }
            KeyButton(text = "%", modifier = Modifier.weight(1f), backgroundColor = opColor) { onOperatorClick("%") }
            KeyButton(text = "÷", modifier = Modifier.weight(1f), backgroundColor = opColor, contentColor = accentColor) { onOperatorClick("/") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(text = "7", modifier = Modifier.weight(1f)) { onDigitClick("7") }
            KeyButton(text = "8", modifier = Modifier.weight(1f)) { onDigitClick("8") }
            KeyButton(text = "9", modifier = Modifier.weight(1f)) { onDigitClick("9") }
            KeyButton(text = "×", modifier = Modifier.weight(1f), backgroundColor = opColor, contentColor = accentColor) { onOperatorClick("*") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(text = "4", modifier = Modifier.weight(1f)) { onDigitClick("4") }
            KeyButton(text = "5", modifier = Modifier.weight(1f)) { onDigitClick("5") }
            KeyButton(text = "6", modifier = Modifier.weight(1f)) { onDigitClick("6") }
            KeyButton(text = "−", modifier = Modifier.weight(1f), backgroundColor = opColor, contentColor = accentColor) { onOperatorClick("-") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(text = "1", modifier = Modifier.weight(1f)) { onDigitClick("1") }
            KeyButton(text = "2", modifier = Modifier.weight(1f)) { onDigitClick("2") }
            KeyButton(text = "3", modifier = Modifier.weight(1f)) { onDigitClick("3") }
            KeyButton(text = "+", modifier = Modifier.weight(1f), backgroundColor = opColor, contentColor = accentColor) { onOperatorClick("+") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(text = "0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButton(text = ".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButton(
                icon = Icons.AutoMirrored.Filled.Backspace, 
                modifier = Modifier.weight(1f),
                backgroundColor = Color.Transparent,
                contentColor = Color.White.copy(alpha = 0.6f)
            ) { onDeleteClick() }
            KeyButton(text = "=", modifier = Modifier.weight(1f), backgroundColor = equalsColor, contentColor = Color.White) { onEqualsClick() }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    text: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF161B22),
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun evaluate(expression: String): Double {
    val cleanExpr = expression.trim().replace("÷", "/").replace("×", "*").replace("−", "-")
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
                } else if (eat('%'.toInt())) {
                    val next = parseFactor()
                    x = (x * next) / 100.0
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
