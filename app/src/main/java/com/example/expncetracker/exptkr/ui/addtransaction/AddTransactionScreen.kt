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
import androidx.compose.foundation.shape.CircleShape
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
            selectedType = it.type
            selectedCategoryName = it.categoryName
            transactionDate = it.timestamp
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
                                amount < 0 -> {
                                    Toast.makeText(context, "Invalid expression or division by zero", Toast.LENGTH_SHORT).show()
                                }
                                amount == 0.0 -> {
                                    Toast.makeText(context, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                                }
                                merchantName.isBlank() -> {
                                    Toast.makeText(context, "Please enter a merchant or payee", Toast.LENGTH_SHORT).show()
                                }
                                selectedCategoryName.isEmpty() -> {
                                    Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    viewModel.addTransaction(
                                        id = transactionId ?: 0L,
                                        amount = amount,
                                        type = selectedType,
                                        category = selectedCategoryName,
                                        description = merchantName.trim(),
                                        bankName = selectedAccount?.name ?: "Manual",
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
            ) {
                // Transaction Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeTab("INCOME", selectedType == TransactionType.CREDIT) { 
                        selectedType = TransactionType.CREDIT 
                        val incomeCats = allCategories.filter { it.type == "INCOME" }
                        if (incomeCats.none { it.name == selectedCategoryName }) {
                            selectedCategoryName = incomeCats.firstOrNull()?.name ?: ""
                        }
                    }
                    Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    TypeTab("EXPENSE", selectedType == TransactionType.DEBIT) { 
                        selectedType = TransactionType.DEBIT 
                        val expenseCats = allCategories.filter { it.type == "EXPENSE" }
                        if (expenseCats.none { it.name == selectedCategoryName }) {
                            selectedCategoryName = expenseCats.firstOrNull()?.name ?: ""
                        }
                    }
                    Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    TypeTab("TRANSFER", selectedType == TransactionType.TRANSFER) { 
                        selectedType = TransactionType.TRANSFER 
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Account and Category Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SelectorItem(
                        label = "Account",
                        value = selectedAccount?.name ?: "Select Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    ) { showAccountSheet = true }
                    
                    SelectorItem(
                        label = "Category",
                        value = selectedCategoryName,
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f)
                    ) { showCategorySheet = true }
                }

                Spacer(Modifier.height(24.dp))

                // Amount Display
                val formattedAmount = remember(amountText) {
                    if (amountText.isEmpty()) "0"
                    else if (amountText.any { it in "+-*/" }) amountText
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formattedAmount,
                            style = when {
                                formattedAmount.length <= 8 -> MaterialTheme.typography.displayMedium
                                formattedAmount.length <= 11 -> MaterialTheme.typography.headlineLarge
                                else -> MaterialTheme.typography.headlineMedium
                            },
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (amountText.length > 1) {
                                amountText = amountText.dropLast(1)
                            } else {
                                amountText = "0"
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Merchant Field
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Where did you spend?") },
                    label = { Text("Merchant / Payee") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )

                Spacer(Modifier.height(12.dp))

                // Note Field (Optional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Add a note... (Optional)") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )

                Spacer(Modifier.height(16.dp))

                // Calculator Keypad
                CalculatorKeypad(
                    modifier = Modifier.weight(1f),
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText == "0") amountText = digit
                        else if (amountText.length < 12) amountText += digit
                    },
                    onOperatorClick = { op ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (amountText.isNotEmpty() && !amountText.last().isDigit() && amountText.last() != '.') {
                            amountText = amountText.dropLast(1) + op
                        } else if (amountText.isNotEmpty() && amountText.last() != '.') {
                            amountText += op
                        }
                    },
                    onDecimalClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val lastPart = amountText.split('+', '-', '*', '/').last()
                        if (!lastPart.contains(".")) {
                            amountText += if (amountText.isEmpty() || !amountText.last().isDigit()) "0." else "."
                        }
                    },
                    onEqualsClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            val result = evaluate(amountText)
                            amountText = if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(result)
                        } catch (_: Exception) { }
                    }
                )
            }

            // Bottom Date/Time bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(transactionDate.format(dateFormatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(transactionDate.format(timeFormatter), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

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
        ) {
            DatePicker(state = datePickerState)
        }
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
                TransactionType.TRANSFER -> allCategories
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
                    Text("No accounts found. Add one in the Accounts screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAccount = account
                                showAccountSheet = false
                            }
                            .padding(vertical = 12.dp),
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
        modifier = Modifier.clickable { onClick() }.padding(vertical = 8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
    )
}

@Composable
fun SelectorItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onClick() },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
            KeyButton("-", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("-") }
        }
        Row(modifier = rowModifier) {
            KeyButton(".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButton("0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButton("=", modifier = Modifier.weight(1f), isAction = true) { onEqualsClick() }
            KeyButton("+", modifier = Modifier.weight(1f), isOperator = true) { onOperatorClick("+") }
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
    val contentColor = when {
        isAction -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = contentColor,
            fontWeight = if (isOperator || isAction) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun evaluate(expression: String): Double {
    val cleanExpr = expression.trim().replace("÷", "/").replace("×", "*")
    if (cleanExpr.isEmpty()) return 0.0

    try {
        // Step 1: Tokenize
        val tokens = mutableListOf<String>()
        var current = ""
        val operators = setOf('+', '-', '*', '/')
        
        for (char in cleanExpr) {
            if (char in operators) {
                if (current.isNotEmpty()) tokens.add(current)
                tokens.add(char.toString())
                current = ""
            } else if (char.isDigit() || char == '.') {
                current += char
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        if (tokens.isEmpty()) return 0.0

        // Handle trailing operator: If last token is an operator, ignore it for calculation
        val processedTokens = if (tokens.last() in setOf("+", "-", "*", "/")) {
            tokens.dropLast(1)
        } else tokens

        if (processedTokens.isEmpty()) return 0.0

        // Step 2: Multiplication and Division (MD)
        val afterMD = mutableListOf<String>()
        var i = 0
        while (i < processedTokens.size) {
            val token = processedTokens[i]
            if (token == "*" || token == "/") {
                val left = afterMD.removeAt(afterMD.size - 1).toDouble()
                val right = processedTokens[++i].toDouble()
                if (token == "/" && right == 0.0) throw ArithmeticException("Div by zero")
                afterMD.add((if (token == "*") left * right else left / right).toString())
            } else {
                afterMD.add(token)
            }
            i++
        }

        // Step 3: Addition and Subtraction (AS)
        var result = afterMD[0].toDouble()
        var j = 1
        while (j < afterMD.size) {
            val op = afterMD[j]
            val next = afterMD[++j].toDouble()
            result = if (op == "+") result + next else result - next
            j++
        }
        return result
    } catch (e: Exception) {
        throw e
    }
}

@Composable
private fun getCategoryColor(category: Category): Color {
    val isDark = MaterialTheme.isDark
    return when (category) {
        Category.FOOD -> if (isDark) CategoryFoodDark else CategoryFood
        Category.CABS -> if (isDark) CategoryCabsDark else CategoryCabs
        Category.RENT -> if (isDark) CategoryRentDark else CategoryRent
        Category.BILLS -> if (isDark) CategoryBillsDark else CategoryBills
        Category.SHOPPING -> if (isDark) CategoryShoppingDark else CategoryShopping
        Category.SALARY -> if (isDark) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS -> if (isDark) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL -> if (isDark) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> if (isDark) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES -> if (isDark) CategoryGroceriesDark else CategoryGroceries
        Category.HEALTHCARE -> if (isDark) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDark) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDark) CategoryOthersDark else CategoryOthers
    }
}
