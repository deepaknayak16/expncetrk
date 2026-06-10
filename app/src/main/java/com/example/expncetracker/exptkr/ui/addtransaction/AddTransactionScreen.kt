package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.ui.theme.LightBackground
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary
import com.example.expncetracker.exptkr.ui.theme.LightBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategory by remember { mutableStateOf("Shopping") }
    
    val categories = listOf("Shopping", "Food", "Transport", "Entertainment", "Bills", "Salary", "Investment", "Travel", "Others")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightTextPrimary)
            }
            Text(
                text = "Add Transaction",
                color = LightTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Amount Input
        Text("Amount", color = LightTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            placeholder = { Text("₹0.00", color = LightTextSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LightPrimary,
                unfocusedBorderColor = LightBorder,
                focusedTextColor = LightTextPrimary,
                unfocusedTextColor = LightTextPrimary
            ),
            leadingIcon = {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = LightPrimary)
            },
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Type Selection
        Text("Type", color = LightTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransactionType.values().forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) if (type == TransactionType.CREDIT) Color(0xFF10B981) else Color(0xFFEF4444) else LightSurface)
                        .clickable { selectedType = type }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (type == TransactionType.DEBIT) "Expense" else "Income",
                        color = if (isSelected) Color.White else LightTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category Selection
        Text("Category", color = LightTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category, color = if (isSelected) Color.White else LightTextSecondary) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LightPrimary,
                        containerColor = LightSurface,
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Description Input
        Text("Description (Optional)", color = LightTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("Enter details...", color = LightTextSecondary.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LightPrimary,
                unfocusedBorderColor = LightBorder,
                focusedTextColor = LightTextPrimary,
                unfocusedTextColor = LightTextPrimary
            ),
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null, tint = LightPrimary)
            },
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Save Button
        Button(
            onClick = {
                if (amount.isNotBlank()) {
                    viewModel.addTransaction(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        type = selectedType,
                        category = selectedCategory,
                        description = description.ifBlank { null }
                    )
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = amount.isNotBlank()
        ) {
            Text(
                text = "Save Transaction",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
