package com.example.expncetracker.exptkr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expncetracker.exptkr.ui.theme.LightBackground
import com.example.expncetracker.exptkr.ui.theme.LightSurface
import com.example.expncetracker.exptkr.ui.theme.LightPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextPrimary
import com.example.expncetracker.exptkr.ui.theme.LightTextSecondary
import com.example.expncetracker.exptkr.ui.theme.LightBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(viewModel: TransactionViewModel) {
    val list by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(LightBackground).padding(16.dp)) {
        Text("Statement Ledger", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LightTextPrimary)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search transactions...", color = LightTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightTextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LightPrimary,
                unfocusedBorderColor = LightBorder,
                focusedContainerColor = LightSurface,
                unfocusedContainerColor = LightSurface,
                focusedTextColor = LightTextPrimary,
                unfocusedTextColor = LightTextPrimary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { tx ->
                TransactionItemImproved(transaction = tx)
            }
        }
    }
}