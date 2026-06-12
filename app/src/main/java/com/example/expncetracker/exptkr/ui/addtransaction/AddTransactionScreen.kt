package com.example.expncetracker.exptkr.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    val isDark = MaterialTheme.isDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("CANCEL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("SAVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
            TypeTab("EXPENSE", selectedType == TransactionType.DEBIT) { selectedType = TransactionType.DEBIT }
            Text("|", color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 8.dp))
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
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CategoryGrid(
                    isDark = isDark,
                    onCategorySelected = {
                        selectedCategory = it
                        showCategorySheet = false
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Amount Display
        val formattedAmount = remember(amountText) {
            if (amountText.isEmpty()) "0"
            else if (amountText.any { it in "+-*/" }) amountText // Don't format expressions yet
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

        val dynamicFontSize = when {
            formattedAmount.length <= 8 -> 48.sp
            formattedAmount.length <= 11 -> 36.sp
            formattedAmount.length <= 14 -> 28.sp
            else -> 22.sp
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(48.dp))
            Text(
                text = formattedAmount,
                style = TextStyle(
                    fontSize = dynamicFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (amountText.length > 1) {
                    amountText = amountText.dropLast(1)
                } else {
                    amountText = "0"
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Note Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .padding(horizontal = 12.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text("note...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                    }
                    innerTextField()
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Calculator Keypad
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
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = transactionDate.format(dateFormatter),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { datePickerDialog.show() }
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(
                text = transactionDate.format(timeFormatter),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .clickable { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
            KeyButton("+", modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.primaryContainer), textColor = MaterialTheme.colorScheme.onPrimaryContainer) { onOperatorClick("+") }
            KeyButton("7", modifier = Modifier.weight(1f)) { onDigitClick("7") }
            KeyButton("8", modifier = Modifier.weight(1f)) { onDigitClick("8") }
            KeyButton("9", modifier = Modifier.weight(1f)) { onDigitClick("9") }
        }
        Row(modifier = rowModifier) {
            KeyButton("-", modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.primaryContainer), textColor = MaterialTheme.colorScheme.onPrimaryContainer) { onOperatorClick("-") }
            KeyButton("4", modifier = Modifier.weight(1f)) { onDigitClick("4") }
            KeyButton("5", modifier = Modifier.weight(1f)) { onDigitClick("5") }
            KeyButton("6", modifier = Modifier.weight(1f)) { onDigitClick("6") }
        }
        Row(modifier = rowModifier) {
            KeyButton("×", modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.primaryContainer), textColor = MaterialTheme.colorScheme.onPrimaryContainer) { onOperatorClick("*") }
            KeyButton("1", modifier = Modifier.weight(1f)) { onDigitClick("1") }
            KeyButton("2", modifier = Modifier.weight(1f)) { onDigitClick("2") }
            KeyButton("3", modifier = Modifier.weight(1f)) { onDigitClick("3") }
        }
        Row(modifier = rowModifier) {
            KeyButton("÷", modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.primaryContainer), textColor = MaterialTheme.colorScheme.onPrimaryContainer) { onOperatorClick("/") }
            KeyButton("0", modifier = Modifier.weight(1f)) { onDigitClick("0") }
            KeyButton(".", modifier = Modifier.weight(1f)) { onDecimalClick() }
            KeyButton("=", modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.primary), textColor = MaterialTheme.colorScheme.onPrimary) { onEqualsClick() }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

private fun evaluate(expression: String): Double {
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
fun CategoryGrid(isDark: Boolean, onCategorySelected: (Category) -> Unit) {
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
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onCategorySelected(category) }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(category, isDark)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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

private fun getCategoryColor(category: Category, isDarkTheme: Boolean): Color {
    return when (category) {
        Category.FOOD -> if (isDarkTheme) CategoryFoodDark else CategoryFood
        Category.CABS -> if (isDarkTheme) CategoryCabsDark else CategoryCabs
        Category.RENT -> if (isDarkTheme) CategoryRentDark else CategoryRent
        Category.BILLS -> if (isDarkTheme) CategoryBillsDark else CategoryBills
        Category.SHOPPING -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.SALARY -> if (isDarkTheme) CategorySalaryDark else CategorySalary
        Category.INVESTMENTS -> if (isDarkTheme) CategoryInvestmentsDark else CategoryInvestments
        Category.TRAVEL -> if (isDarkTheme) CategoryTravelDark else CategoryTravel
        Category.ENTERTAINMENT -> if (isDarkTheme) CategoryEntertainmentDark else CategoryEntertainment
        Category.GROCERIES -> if (isDarkTheme) CategoryShoppingDark else CategoryShopping
        Category.HEALTHCARE -> if (isDarkTheme) CategoryHealthDark else CategoryHealth
        Category.EDUCATION -> if (isDarkTheme) CategoryEducationDark else CategoryEducation
        Category.OTHERS -> if (isDarkTheme) CategoryOthersDark else CategoryOthers
    }
}
