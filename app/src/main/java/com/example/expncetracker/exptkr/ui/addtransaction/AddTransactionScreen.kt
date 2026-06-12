package com.example.expncetracker.exptkr.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

// Specific colors to match the image
private val CalculatorBg = Color(0xFFFEFBEA)
private val CalculatorGreen = Color(0xFF2D5D4E)
private val CalculatorGreenLight = Color(0xFF6E9185)
private val ButtonBorder = Color(0xFFB4C4BD)

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

    val context = LocalContext.current
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM dd, yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                transactionDate = LocalDateTime.of(
                    LocalDate.of(year, month + 1, dayOfMonth),
                    transactionDate.toLocalTime()
                )
            },
            transactionDate.year,
            transactionDate.monthValue - 1,
            transactionDate.dayOfMonth
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                transactionDate = LocalDateTime.of(
                    transactionDate.toLocalDate(),
                    LocalTime.of(hourOfDay, minute)
                )
            },
            transactionDate.hour,
            transactionDate.minute,
            false
        )
    }

    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatorBg)
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onNavigateBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = CalculatorGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("CANCEL", color = CalculatorGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.clickable {
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
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = CalculatorGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("SAVE", color = CalculatorGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Transaction Type Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeTab("INCOME", selectedType == TransactionType.CREDIT) { selectedType = TransactionType.CREDIT }
            Text("|", color = ButtonBorder, modifier = Modifier.padding(horizontal = 8.dp))
            TypeTab("EXPENSE", selectedType == TransactionType.DEBIT) { selectedType = TransactionType.DEBIT }
            Text("|", color = ButtonBorder, modifier = Modifier.padding(horizontal = 8.dp))
            TypeTab("TRANSFER", selectedType == TransactionType.TRANSFER) { selectedType = TransactionType.TRANSFER }
        }

        Spacer(Modifier.height(16.dp))

        // Account and Category Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectorItem(
                label = "Account",
                value = selectedAccount,
                icon = Icons.Default.CreditCard,
                modifier = Modifier.weight(1f)
            ) { /* Show Account Picker */ }
            
            SelectorItem(
                label = "Category",
                value = selectedCategory.displayName,
                icon = getCategoryIcon(selectedCategory),
                modifier = Modifier.weight(1f)
            ) { showCategorySheet = true }
        }

        if (showCategorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showCategorySheet = false },
                sheetState = sheetState,
                containerColor = CalculatorBg
            ) {
                CategoryGrid(
                    onCategorySelected = {
                        selectedCategory = it
                        showCategorySheet = false
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Amount Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(1.dp, ButtonBorder, RoundedCornerShape(4.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp)) // To center the amount
            Text(
                text = amountText,
                style = TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    color = CalculatorGreen,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (amountText.length > 1) {
                    amountText = amountText.dropLast(1)
                } else {
                    amountText = "0"
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = CalculatorGreen, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Note Field (making this scrollable and flexible)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .padding(horizontal = 12.dp)
                .border(1.dp, ButtonBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(fontSize = 16.sp, color = CalculatorGreen),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text("note...", color = CalculatorGreen.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Calculator Keypad (Giving more weight to keypad)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            CalculatorKeypad(
                onDigitClick = { digit ->
                    if (amountText == "0") {
                        amountText = digit
                    } else if (amountText.length < 12) {
                        amountText += digit
                    }
                },
                onOperatorClick = { op ->
                    if (!amountText.endsWith("+") && !amountText.endsWith("-") && 
                        !amountText.endsWith("*") && !amountText.endsWith("/")) {
                        amountText += op
                    }
                },
                onDecimalClick = {
                    if (!amountText.contains(".")) {
                        amountText += "."
                    }
                },
                onEqualsClick = {
                    try {
                        val result = evaluate(amountText)
                        amountText = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(result)
                    } catch (_: Exception) {
                    }
                }
            )
        }

        // Bottom Date/Time bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CalculatorBg)
                .border(1.dp, ButtonBorder)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = transactionDate.format(dateFormatter),
                color = CalculatorGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { datePickerDialog.show() }
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(ButtonBorder))
            Text(
                text = transactionDate.format(timeFormatter),
                color = CalculatorGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { timePickerDialog.show() }
            )
        }
    }
}

@Composable
fun TypeTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(CalculatorGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = if (isSelected) CalculatorGreen else CalculatorGreenLight,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
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
            color = CalculatorGreenLight,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ButtonBorder, RoundedCornerShape(4.dp))
                .clickable { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(0.5.dp, ButtonBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(value, color = CalculatorGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun CalculatorKeypad(
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onEqualsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val rowModifier = Modifier.fillMaxWidth().weight(1f)
        
        Row(modifier = rowModifier) {
            KeyButton("+", modifier = Modifier.weight(1f).background(CalculatorGreenLight), textColor = Color.White) { onOperatorClick("+") }
            KeyButton("7", modifier = Modifier.weight(1f)) { onDigitClick("7") }
            KeyButton("8", modifier = Modifier.weight(1f)) { onDigitClick("8") }
            KeyButton("9", modifier = Modifier.weight(1f)) { onDigitClick("9") }
        }
        Row(modifier = rowModifier) {
            KeyButton("-", modifier = Modifier.weight(1f).background(CalculatorGreenLight), textColor = Color.White) { onOperatorClick("-") }
            KeyButton("4", modifier = Modifier.weight(1f)) { onDigitClick("4") }
            KeyButton("5", modifier = Modifier.weight(1f)) { onDigitClick("5") }
            KeyButton("6", modifier = Modifier.weight(1f)) { onDigitClick("6") }
        }
        Row(modifier = rowModifier) {
            KeyButton("×", modifier = Modifier.weight(1f).background(CalculatorGreenLight), textColor = Color.White) { onOperatorClick("*") }
            KeyButton("1", modifier = Modifier.weight(1f)) { onDigitClick("1") }
            KeyButton("2", modifier = Modifier.weight(1f)) { onDigitClick("2") }
            KeyButton("3", modifier = Modifier.weight(1f)) { onDigitClick("3") }
        }
        Row(modifier = rowModifier) {
            KeyButton("÷", modifier = Modifier.weight(1f).background(CalculatorGreenLight), textColor = Color.White) { onOperatorClick("/") }
            KeyButton("0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButton(".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButton("=", modifier = Modifier.weight(1f).background(CalculatorGreenLight), textColor = Color.White) { onEqualsClick() }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = CalculatorGreen,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, ButtonBorder)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

private fun evaluate(expression: String): Double {
    // Very basic evaluator for +, -, *, /
    val operators = listOf('+', '-', '*', '/')
    val opIndex = expression.indexOfAny(operators.toCharArray())
    if (opIndex == -1) return expression.toDoubleOrNull() ?: 0.0
    
    val op = expression[opIndex]
    val left = expression.substring(0, opIndex).toDoubleOrNull() ?: 0.0
    val right = expression.substring(opIndex + 1).toDoubleOrNull() ?: 0.0
    
    return when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right != 0.0) left / right else 0.0
        else -> left
    }
}

@Composable
fun CategoryGrid(onCategorySelected: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(Category.entries) { category ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategorySelected(category) }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, ButtonBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.displayName,
                    fontSize = 12.sp,
                    color = CalculatorGreen,
                    textAlign = TextAlign.Center
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

