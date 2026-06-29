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
import androidx.compose.ui.draw.alpha
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val transactionToEdit by viewModel.transactionToEdit.collectAsStateWithLifecycle()
    // Lockdown Mode: If SMS and category is "Other"
    val isRestrictedSms = remember(transactionToEdit) {
        val tx = transactionToEdit
        val isSms = tx?.smsId != null
        val cat = tx?.categoryName?.trim()?.lowercase() ?: ""
        val isOthers = cat == "other" || cat == "others"
        isSms && isOthers
    }
    val isSms = transactionToEdit?.smsId != null
    
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val suggestedCategory by viewModel.suggestedCategory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearEdit()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.statusEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

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
        if (transactionId != null && transactionId != 0L) {
            viewModel.loadTransaction(transactionId) // FIX #5: was viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(transactionToEdit) {
        transactionToEdit?.let {
            amountText = if (it.amount.remainder(BigDecimal.ONE).signum() == 0) it.amount.toBigInteger().toString() else "%.2f".format(it.amount)
            merchantName = it.merchant
            note = it.note ?: ""
            selectedType = it.type
            selectedCategoryName = it.categoryName
            transactionDate = it.timestamp
            counterparty = it.counterparty ?: ""
            isRecurring = it.isRecurring
            it.frequency?.let { f -> recurrenceFrequency = f }
            tagsInput = it.tags.joinToString(", ")

            // Trigger category suggestion for "Other" SMS transactions immediately on load
            val cat = it.categoryName.trim().lowercase()
            val isOthers = cat == "other" || cat == "others"
            if (it.smsId != null && isOthers) {
                viewModel.onMerchantNameChanged(it.merchant, it.categoryName, false, it.type)
            }
        }
    }
    LaunchedEffect(suggestedCategory) {
        suggestedCategory?.let {
            val cat = selectedCategoryName.trim().lowercase()
            val isOthers = cat == "other" || cat == "others"
            if (!userSelectedCategory && (selectedCategoryName.isBlank() || isOthers)) {
                selectedCategoryName = it
            }
        }
    }
    LaunchedEffect(allCategories) {
        if (selectedCategoryName.isEmpty() && allCategories.isNotEmpty())
            selectedCategoryName = allCategories.find { it.type == "EXPENSE" }?.name ?: ""
    }
    LaunchedEffect(accounts, transactionToEdit) {
        selectedAccount = if (transactionToEdit != null)
            accounts.find { it.id == transactionToEdit?.accountId }
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
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
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
                                val tagList = tagsInput
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { if (it.startsWith("#")) it else "#$it" }

                                viewModel.addTransaction(
                                    id = transactionId ?: 0L,
                                    amountText = amountText,
                                    type = selectedType,
                                    category = selectedCategoryName,
                                    merchant = merchantName.trim(),
                                    note = note.trim(),
                                    bankName = selectedAccount?.name ?: "",
                                    accountId = selectedAccount?.id ?: 0L,
                                    counterparty = counterparty.trim().ifEmpty { null },
                                    isRecurring = isRecurring,
                                    frequency = if (isRecurring) recurrenceFrequency else null,
                                    tags = tagList,
                                    timestamp = transactionDate
                                )
                            },
                            enabled = viewModel.isFormValid(
                                amountText,
                                merchantName,
                                selectedCategoryName,
                                selectedAccount?.name ?: ""
                            ) && !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isRestrictedSms) "Reclassify & Save" else "Save",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ──────────────────────────────────────────────
            // LOCKDOWN BANNER
            // ──────────────────────────────────────────────
            if (isRestrictedSms) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bank data locked. You can only update the category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Form area (scrollable) ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Type selector ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            enabled = !isRestrictedSms,
                            selectedColor = when (type) {
                                TransactionType.CREDIT -> MaterialTheme.colorScheme.primary
                                TransactionType.DEBIT -> MaterialTheme.colorScheme.error
                                TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
                                TransactionType.LEND -> MaterialTheme.colorScheme.tertiary
                                TransactionType.BORROW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            },
                            onClick = {
                                selectedType = type
                                when (type) {
                                    TransactionType.CREDIT ->
                                        selectedCategoryName = allCategories.find { it.type == "INCOME" }?.name ?: ""
                                    TransactionType.DEBIT ->
                                        selectedCategoryName = allCategories.find { it.type == "EXPENSE" }?.name ?: ""
                                    TransactionType.TRANSFER ->
                                        selectedCategoryName = allCategories.find { it.name == "Transfer" }?.name ?: "Others"
                                    else ->
                                        selectedCategoryName = "Others"
                                }
                                userSelectedCategory = false
                            }
                        )
                    }
                }

                // ── Account + Category selectors ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DropdownSelector(
                        value = selectedAccount?.name ?: "Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        enabled = !isRestrictedSms,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }

                    DropdownSelector(
                        value = selectedCategoryName.ifEmpty { "Category" },
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f),
                        isSuggested = suggestedCategory != null && selectedCategoryName == suggestedCategory
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(10.dp))

                // ── Amount display ──────────────────────────────────────────
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = typeColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.displaySmall,
                            color = typeColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.displaySmall,
                            color = typeColor,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Text fields ─────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            viewModel.onMerchantNameChanged(it, selectedCategoryName, userSelectedCategory, selectedType)
                        },
                        placeholder = "Merchant / payee",
                        enabled = !isRestrictedSms,
                        leadingIcon = Icons.Default.Store
                    )

                    FormField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "Note (optional)",
                        enabled = !isRestrictedSms,
                        leadingIcon = Icons.AutoMirrored.Filled.Notes
                    )

                    FormField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        placeholder = "Labels / tags",
                        enabled = !isRestrictedSms,
                        leadingIcon = Icons.AutoMirrored.Filled.Label
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── Recurring toggle ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                        enabled = !isRestrictedSms,
                        modifier = Modifier.scale(0.8f)
                    )
                }

                // Frequency chips — shown inline when recurring is on
                AnimatedVisibility(visible = isRecurring) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
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
                    .alpha(if (isSms) 0.8f else 1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                // Date / time row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDatePicker = true },
                        enabled = !isRestrictedSms,
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
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    TextButton(
                        onClick = { showTimePicker = true },
                        enabled = !isRestrictedSms,
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
                        .height(248.dp)
                        .padding(horizontal = 8.dp)
                        .navigationBarsPadding(),
                    enabled = !isRestrictedSms,
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
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
    enabled: Boolean = true,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) selectedColor else Color.Transparent,
            border = if (isSelected) null
            else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                color = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── DropdownSelector ──────────────────────────────────────────────────────────

@Composable
fun DropdownSelector(
    value: String,
    icon: ImageVector,
    enabled: Boolean = true,
    isSuggested: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isSuggested) 1.5.dp else 0.5.dp,
            color = if (isSuggested) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(16.dp),
                tint = if (isSuggested) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                if (isSuggested) {
                    Text(
                        "SUGGESTED",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
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
    enabled: Boolean = true,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBracketClick: () -> Unit
) {
    val opBg = MaterialTheme.colorScheme.secondaryContainer
    val opFg = MaterialTheme.colorScheme.onSecondaryContainer
    val accentFg = MaterialTheme.colorScheme.primary
    val clearBg = MaterialTheme.colorScheme.errorContainer
    val clearFg = MaterialTheme.colorScheme.onErrorContainer
    val eqBg = MaterialTheme.colorScheme.primary
    val eqFg = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .alpha(if (enabled) 1f else 0.5f)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(8.dp)
    ) {
        val rowMod = Modifier
            .fillMaxWidth()
            .weight(1f)
        val gap = 6.dp

        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("AC", Modifier.weight(1f), clearBg, clearFg, enabled = enabled) { onClearClick() }
            CalcKey("( )", Modifier.weight(1f), opBg, opFg, enabled = enabled) { onBracketClick() }
            CalcKey("%", Modifier.weight(1f), opBg, opFg, enabled = enabled) { onOperatorClick("%") }
            CalcKey("÷", Modifier.weight(1f), opBg, accentFg, enabled = enabled) { onOperatorClick("/") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("7", Modifier.weight(1f), enabled = enabled) { onDigitClick("7") }
            CalcKey("8", Modifier.weight(1f), enabled = enabled) { onDigitClick("8") }
            CalcKey("9", Modifier.weight(1f), enabled = enabled) { onDigitClick("9") }
            CalcKey("×", Modifier.weight(1f), opBg, accentFg, enabled = enabled) { onOperatorClick("*") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("4", Modifier.weight(1f), enabled = enabled) { onDigitClick("4") }
            CalcKey("5", Modifier.weight(1f), enabled = enabled) { onDigitClick("5") }
            CalcKey("6", Modifier.weight(1f), enabled = enabled) { onDigitClick("6") }
            CalcKey("−", Modifier.weight(1f), opBg, accentFg, enabled = enabled) { onOperatorClick("-") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("1", Modifier.weight(1f), enabled = enabled) { onDigitClick("1") }
            CalcKey("2", Modifier.weight(1f), enabled = enabled) { onDigitClick("2") }
            CalcKey("3", Modifier.weight(1f), enabled = enabled) { onDigitClick("3") }
            CalcKey("+", Modifier.weight(1f), opBg, accentFg, enabled = enabled) { onOperatorClick("+") }
        }
        Spacer(Modifier.height(gap))
        Row(rowMod, horizontalArrangement = Arrangement.spacedBy(gap)) {
            CalcKey("0", Modifier.weight(1f), enabled = enabled) { onDigitClick("0") }
            CalcKey(".", Modifier.weight(1f), enabled = enabled) { onDecimalClick() }
            CalcKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                modifier = Modifier.weight(1f),
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = enabled
            ) { onDeleteClick() }
            CalcKey("=", Modifier.weight(1f), eqBg, eqFg, enabled = enabled) { onEqualsClick() }
        }
    }
}

@Composable
private fun RowScope.CalcKey(
    text: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(100), label = ""
    )
    Surface(
        onClick = {
            if (enabled) {
                isPressed = true
                onClick()
                coroutineScope.launch { delay(100); isPressed = false }
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(10.dp),
        enabled = enabled,
        color = backgroundColor
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
