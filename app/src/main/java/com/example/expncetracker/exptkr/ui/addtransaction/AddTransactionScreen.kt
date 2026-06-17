package com.example.expncetracker.exptkr.ui.addtransaction

import android.R
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (idx != -1) counterparty = c.getString(idx)
                }
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0L) viewModel.loadTransaction(transactionId)
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
            it.frequency?.let { f -> recurrenceFrequency = f }
            tagsInput = it.tags.joinToString(", ")
        }
    }
    LaunchedEffect(suggestedCategory) {
        suggestedCategory?.let {
            if (!userSelectedCategory && (selectedCategoryName.isEmpty() || selectedCategoryName == "Others"))
                selectedCategoryName = it
        }
    }
    LaunchedEffect(allCategories) {
        if (selectedCategoryName.isEmpty() && allCategories.isNotEmpty())
            selectedCategoryName = allCategories.find { it.type == "EXPENSE" }?.name ?: ""
    }
    LaunchedEffect(accounts, transactionToEdit) {
        selectedAccount = if (transactionToEdit != null)
            accounts.find { it.name == transactionToEdit?.bankName }
        else
            accounts.firstOrNull()
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

    // Formatted display amount
    val displayAmount = remember(amountText) {
        if (amountText.isEmpty()) "0"
        else if (amountText.any { it in "+-*/()" }) amountText
        else runCatching {
            val parts = amountText.split(".")
            val formatted = java.text.NumberFormat.getNumberInstance(Locale.US)
                .format(parts[0].toLongOrNull() ?: 0L)
            if (parts.size > 1) "$formatted.${parts[1]}" else formatted
        }.getOrDefault(amountText)
    }

    // Type → accent color
    val typeColor = when (selectedType) {
        TransactionType.CREDIT -> Color(0xFF10B981) // Vibrant Green
        TransactionType.DEBIT -> Color(0xFFEF4444)  // Vibrant Red
        TransactionType.TRANSFER -> Color(0xFF3B82F6) // Vibrant Blue
        TransactionType.LEND -> Color(0xFFF97316)    // Vibrant Orange
        TransactionType.BORROW -> Color(0xFF8B5CF6)  // Vibrant Purple
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (transactionId != null && transactionId != 0L) "Edit transaction"
                        else "Add transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint= Color.Red
                        )

                    }
                },
                actions = {
                    // FIX: inline save button instead of bare icon — clearer affordance
                        Button(
                            onClick = {
                                val amount = runCatching {
                                    evaluate(amountText)
                                }.getOrDefault(-1.0)

                                when {
                                    amount < 0 ->
                                        Toast.makeText(
                                            context,
                                            "Invalid expression",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    amount == 0.0 ->
                                        Toast.makeText(
                                            context,
                                            "Enter an amount",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    merchantName.isBlank() ->
                                        Toast.makeText(
                                            context,
                                            "Enter merchant",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    selectedAccount == null ->
                                        Toast.makeText(
                                            context,
                                            "Select account",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    else -> {
                                        val tagList = tagsInput
                                            .split(",")
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

                                        Toast.makeText(
                                            context,
                                            "Transaction saved!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onNavigateBack()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Save",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Form area (scrollable) ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Type selector ───────────────────────────────────────────
                // FIX: horizontally scrolling pill row — no weight(1f) squishing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf(
                        TransactionType.CREDIT to "Income",
                        TransactionType.DEBIT to "Expense",
                        TransactionType.TRANSFER to "Transfer",
                        TransactionType.LEND to "Lend",
                        TransactionType.BORROW to "Borrow"
                    )
                    types.forEach { (type, label) ->
                        TypePill(
                            label = label,
                            isSelected = selectedType == type,
                            selectedColor = when (type) {
                                TransactionType.CREDIT -> Color(0xFF10B981)
                                TransactionType.DEBIT -> Color(0xFFEF4444)
                                TransactionType.TRANSFER -> Color(0xFF3B82F6)
                                TransactionType.LEND -> Color(0xFFF97316)
                                TransactionType.BORROW -> Color(0xFF8B5CF6)
                            },
                            onClick = {
                                selectedType = type
                                when (type) {
                                    TransactionType.CREDIT ->
                                        allCategories.find { it.type == "INCOME" }
                                            ?.let { selectedCategoryName = it.name }
                                    TransactionType.DEBIT ->
                                        allCategories.find { it.type == "EXPENSE" }
                                            ?.let { selectedCategoryName = it.name }
                                    else -> {}
                                }
                            }
                        )
                    }
                }

                // ── Account + Category selectors ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownSelector(
                        value = selectedAccount?.name ?: "Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }

                    DropdownSelector(
                        value = selectedCategoryName.ifEmpty { "Category" },
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f)
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(8.dp))

                // ── Amount display ──────────────────────────────────────────
                // FIX: amount uses typeColor so debit = red, credit = green at a glance
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = typeColor.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.headlineMedium,
                            color = typeColor.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.headlineMedium,
                            color = typeColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Text fields ─────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Counterparty (lend/borrow only)
                    if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                        FormField(
                            value = counterparty,
                            onValueChange = { counterparty = it },
                            placeholder = if (selectedType == TransactionType.LEND)
                                "Lent to" else "Borrowed from",
                            leadingIcon = Icons.Default.Person,
                            trailingContent = {
                                IconButton(
                                    onClick = { contactPicker.launch(null) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContactPage,
                                        "Pick contact",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }

                    FormField(
                        value = merchantName,
                        onValueChange = {
                            merchantName = it
                            viewModel.onMerchantNameChanged(it, selectedCategoryName)
                        },
                        placeholder = "Merchant / payee",
                        leadingIcon = Icons.Default.Store
                    )

                    FormField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "Note (optional)",
                        leadingIcon = Icons.AutoMirrored.Filled.Notes
                    )

                    FormField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        placeholder = "Labels / tags",
                        leadingIcon = Icons.AutoMirrored.Filled.Label
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── Recurring toggle ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Recurring transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                // Frequency chips — shown inline when recurring is on
                AnimatedVisibility(visible = isRecurring) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RecurrenceFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = recurrenceFrequency == freq,
                                onClick = { recurrenceFrequency = freq },
                                label = {
                                    Text(
                                        freq.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // ── Fixed bottom: date/time + calculator ────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181111))
            ) {
                // Date / time row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Event, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            transactionDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(14.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    TextButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            transactionDate.format(timeFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Calculator keypad
                CalculatorKeypad(
                    modifier = Modifier
                        .height(268.dp)
                        .padding(horizontal = 8.dp)
                        .navigationBarsPadding(),
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText == "0") amountText = digit
                        else if (amountText.length < 24) amountText += digit
                    },
                    onOperatorClick = { op ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText.isNotEmpty()) {
                            val last = amountText.last()
                            if (!last.isDigit() && last != '.' && last != ')' && last != '(')
                                amountText = amountText.dropLast(1) + op
                            else if (last != '.')
                                amountText += op
                        }
                    },
                    onDecimalClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val lastPart = amountText.split('+', '-', '*', '/', '(', ')').last()
                        if (!lastPart.contains("."))
                            amountText += if (amountText.isEmpty() || !amountText.last().isDigit()) "0." else "."
                    },
                    onEqualsClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        runCatching {
                            val result = evaluate(amountText)
                            amountText = if (result % 1.0 == 0.0) result.toLong().toString()
                            else "%.2f".format(result)
                        }.onFailure {
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
                        val open = amountText.count { it == '(' }
                        val close = amountText.count { it == ')' }
                        amountText += when {
                            amountText.isEmpty() -> "("
                            amountText.last().let { it.isDigit() || it == ')' } ->
                                if (open > close) ")" else "*("
                            else -> "("
                        }
                    }
                )
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        transactionDate = LocalDateTime.of(d, transactionDate.toLocalTime())
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
                    Text(
                        "Select time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
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
                TransactionType.TRANSFER -> allCategories.filter {
                    it.name == "Transfer" || it.name == "Others"
                }
                else -> allCategories
            }

            Text(
                "Select category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoriesToShow, key = { it.name }) { category ->
                    val color = Color(category.color)
                    val icon = getIconByName(category.iconName)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                selectedCategoryName = category.name
                                userSelectedCategory = true
                                showCategorySheet = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        // FIX: highlight ring when selected
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.12f))
                                .then(
                                    if (selectedCategoryName == category.name)
                                        Modifier.border(2.dp, color, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Select account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))

                if (accounts.isEmpty()) {
                    Text(
                        "No accounts found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedAccount = account
                                    showAccountSheet = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(account.color).copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(account.color)
                                    )
                                }
                            }
                            Text(
                                account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            // FIX: show checkmark for currently selected account
                            if (selectedAccount?.name == account.name) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (account != accounts.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── TypePill ──────────────────────────────────────────────────────────────────

@Composable
fun TypePill(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedColor else Color.Transparent,
        border = if (isSelected) null
        else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── DropdownSelector ──────────────────────────────────────────────────────────

@Composable
fun DropdownSelector(
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown, null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── FormField ─────────────────────────────────────────────────────────────────

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingContent: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(leadingIcon, null, Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = trailingContent,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

// ── CalculatorKeypad (unchanged logic, cosmetic tightening) ───────────────────

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
    val opBg = Color(0xFF3F51B5)
    val accentBlue = Color(0xFF60A5FA)
    val clearBg = Color(0xFFEF4444).copy(alpha = 0.15f)
    val clearFg = Color(0xFFEF4444)
    val eqBg = Color(0xFFFFEB3B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFFDA9797))
            .padding(8.dp)
    ) {
        val rowMod = Modifier
            .fillMaxWidth()
            .weight(1f)
        val gap = 6.dp

        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("AC", Modifier.weight(1f), clearBg, clearFg) { onClearClick() }
            CalcKey("( )", Modifier.weight(1f), opBg) { onBracketClick() }
            CalcKey("%", Modifier.weight(1f), opBg) { onOperatorClick("%") }
            CalcKey("÷", Modifier.weight(1f), opBg, accentBlue) { onOperatorClick("/") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("7", Modifier.weight(1f)) { onDigitClick("7") }
            CalcKey("8", Modifier.weight(1f)) { onDigitClick("8") }
            CalcKey("9", Modifier.weight(1f)) { onDigitClick("9") }
            CalcKey("×", Modifier.weight(1f), opBg, accentBlue) { onOperatorClick("*") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("4", Modifier.weight(1f)) { onDigitClick("4") }
            CalcKey("5", Modifier.weight(1f)) { onDigitClick("5") }
            CalcKey("6", Modifier.weight(1f)) { onDigitClick("6") }
            CalcKey("−", Modifier.weight(1f), opBg, accentBlue) { onOperatorClick("-") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("1", Modifier.weight(1f)) { onDigitClick("1") }
            CalcKey("2", Modifier.weight(1f)) { onDigitClick("2") }
            CalcKey("3", Modifier.weight(1f)) { onDigitClick("3") }
            CalcKey("+", Modifier.weight(1f), opBg, accentBlue) { onOperatorClick("+") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("0", Modifier.weight(1f)) { onDigitClick("0") }
            CalcKey(".", Modifier.weight(1f)) { onDecimalClick() }
            CalcKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                modifier = Modifier.weight(1f),
                backgroundColor = Color.Transparent,
                contentColor = Color.Black.copy(alpha = 1.5f)
            ) { onDeleteClick() }
            CalcKey("=", Modifier.weight(1f), eqBg, Color.Black) { onEqualsClick() }
        }
    }
}

@Composable
private fun RowScope.CalcKey(
    text: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF161B22),
    contentColor: Color = Color.Black,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100)
    )
    Surface(
        onClick = {
            isPressed = true
            onClick()
            coroutineScope.launch { delay(100); isPressed = false }
        },
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(10.dp)

    )
    {
        Box(contentAlignment = Alignment.Center) {
            when {
                text != null -> Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                icon != null -> Icon(
                    icon, null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── evaluate() — unchanged ────────────────────────────────────────────────────

private fun evaluate(expression: String): Double {
    val expr = expression.trim()
        .replace("÷", "/")
        .replace("×", "*")
        .replace("−", "-")
    if (expr.isEmpty()) return 0.0
    return object : Any() {
        var pos = -1; var ch = 0
        fun nextChar() { ch = if (++pos < expr.length) expr[pos].code else -1 }
        fun eat(c: Int): Boolean {
            while (ch == ' '.code) nextChar()
            return if (ch == c) { nextChar(); true } else false
        }
        fun parse(): Double { nextChar(); return parseExpr().also { if (pos < expr.length) throw RuntimeException("Unexpected: ${ch.toChar()}") } }
        fun parseExpr(): Double { var x = parseTerm(); while (true) x = when { eat('+'.code) -> x + parseTerm(); eat('-'.code) -> x - parseTerm(); else -> return x }; @Suppress("UNREACHABLE_CODE") return x }
        fun parseTerm(): Double { var x = parseFactor(); while (true) x = when { eat('*'.code) -> x * parseFactor(); eat('/'.code) -> parseFactor().let { d -> if (d == 0.0) throw ArithmeticException("Div by zero"); x / d }; eat('%'.code) -> (x * parseFactor()) / 100.0; else -> return x }; @Suppress("UNREACHABLE_CODE") return x }
        fun parseFactor(): Double { if (eat('+'.code)) return parseFactor(); if (eat('-'.code)) return -parseFactor(); val start = pos; return if (eat('('.code)) parseExpr().also { eat(')'.code) } else if (ch in '0'.code..'9'.code || ch == '.'.code) { while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar(); expr.substring(start, pos).toDouble() } else throw RuntimeException("Unexpected: ${ch.toChar()}") }
    }.parse()
}