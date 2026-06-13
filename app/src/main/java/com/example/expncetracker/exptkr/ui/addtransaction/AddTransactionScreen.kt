package com.example.expncetracker.exptkr.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.theme.*
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
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amountText by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategory by remember { mutableStateOf(Category.SHOPPING) }
    var selectedAccount by remember { mutableStateOf("Card") }
    var transactionDate by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transactionDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = transactionDate.hour,
        initialMinute = transactionDate.minute
    )

    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }

    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Standardized Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.addTransaction(
                            amount = amount,
                            type = selectedType,
                            category = selectedCategory.name,
                            description = note.ifBlank { null },
                            timestamp = transactionDate
                        )
                        onNavigateBack()
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Transaction Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeTab("INCOME", selectedType == TransactionType.CREDIT) { selectedType = TransactionType.CREDIT }
                Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                TypeTab("EXPENSE", selectedType == TransactionType.DEBIT) { selectedType = TransactionType.DEBIT }
                Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                TypeTab("TRANSFER", selectedType == TransactionType.TRANSFER) { selectedType = TransactionType.TRANSFER }
            }

            Spacer(Modifier.height(24.dp))

            // Account and Category Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SelectorItem(
                    label = "Account",
                    value = selectedAccount,
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                ) { /* For simplicity in this fix, we toggle between a few options */
                    val accounts = listOf("Cash", "Bank Card", "Savings", "Credit")
                    val nextIndex = (accounts.indexOf(selectedAccount) + 1) % accounts.size
                    selectedAccount = accounts[nextIndex]
                }
                
                SelectorItem(
                    label = "Category",
                    value = selectedCategory.displayName,
                    icon = getCategoryIcon(selectedCategory),
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

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                placeholder = { Text("Add a note...") },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            // Calculator Keypad
            CalculatorKeypad(
                modifier = Modifier.weight(1f),
                onDigitClick = { digit ->
                    if (amountText == "0") amountText = digit
                    else if (amountText.length < 12) amountText += digit
                },
                onOperatorClick = { op ->
                    if (!amountText.endsWith("+") && !amountText.endsWith("-") && 
                        !amountText.endsWith("*") && !amountText.endsWith("/")) {
                        amountText += op
                    }
                },
                onDecimalClick = {
                    if (!amountText.contains(".")) amountText += "."
                },
                onEqualsClick = {
                    try {
                        val result = evaluate(amountText)
                        amountText = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(result)
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
            CategoryGrid(
                onCategorySelected = {
                    selectedCategory = it
                    showCategorySheet = false
                }
            )
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
    if (expression.isEmpty()) return 0.0
    
    // Simple improvement: evaluate left to right for multiple operators
    val operators = listOf('+', '-', '*', '/')
    var currentExpression = expression.replace("÷", "/").replace("×", "*")
    
    // Handle the case where it starts with an operator
    if (operators.contains(currentExpression.first())) {
        currentExpression = "0$currentExpression"
    }

    try {
        // Regex to split by operators while keeping them
        val tokens = mutableListOf<String>()
        var currentToken = ""
        for (char in currentExpression) {
            if (operators.contains(char)) {
                if (currentToken.isNotEmpty()) tokens.add(currentToken)
                tokens.add(char.toString())
                currentToken = ""
            } else {
                currentToken += char
            }
        }
        if (currentToken.isNotEmpty()) tokens.add(currentToken)

        if (tokens.isEmpty()) return 0.0
        
        var result = tokens[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val nextVal = if (i + 1 < tokens.size) tokens[i+1].toDoubleOrNull() ?: 0.0 else 0.0
            result = when (op) {
                "+" -> result + nextVal
                "-" -> result - nextVal
                "*" -> result * nextVal
                "/" -> if (nextVal != 0.0) result / nextVal else result
                else -> result
            }
            i += 2
        }
        return result
    } catch (e: Exception) {
        return expression.toDoubleOrNull() ?: 0.0
    }
}

@Composable
fun CategoryGrid(onCategorySelected: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Category.entries) { category ->
            val color = getCategoryColor(category)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(90.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onCategorySelected(category) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getCategoryIcon(category: Category): ImageVector {
    return when (category) {
        Category.FOOD -> Icons.Default.Restaurant
        Category.CABS -> Icons.Default.DirectionsCar
        Category.RENT -> Icons.Default.HomeWork
        Category.BILLS -> Icons.Default.Bolt
        Category.SHOPPING -> Icons.Default.LocalMall
        Category.SALARY -> Icons.Default.Payments
        Category.INVESTMENTS -> Icons.AutoMirrored.Filled.TrendingUp
        Category.TRAVEL -> Icons.Default.LocalAirport
        Category.ENTERTAINMENT -> Icons.Default.LiveTv
        Category.HEALTHCARE -> Icons.Default.Favorite
        Category.EDUCATION -> Icons.Default.School
        Category.GROCERIES -> Icons.Default.ShoppingCart
        Category.OTHERS -> Icons.Default.GridView
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
        Category.GROCERIES -> if (isDark) CategoryShoppingDark else CategoryShopping
        Category.HEALTHCARE -> if (isDark) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDark) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDark) CategoryOthersDark else CategoryOthers
    }
}
