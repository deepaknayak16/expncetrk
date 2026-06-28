package com.example.expncetracker.exptkr.ui.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(viewModel: BillsViewModel) {
    val bills by viewModel.bills.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Managed Bills") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bills.filter { it.state != RecurringState.CANCELLED.name }, key = { it.id }) { bill ->
                BillItem(
                    bill = bill,
                    onToggle = { 
                        val nextState = if (bill.state == RecurringState.ACTIVE.name) RecurringState.PAUSED else RecurringState.ACTIVE
                        viewModel.updateBillState(bill.id, nextState)
                    },
                    onDelete = { viewModel.deleteBill(bill.id) }
                )
            }
        }
    }
}

@Composable
fun BillItem(
    bill: RecurringTemplateEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dueDate = Instant.ofEpochMilli(bill.nextDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val formattedDate = dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(bill.merchantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Next due: $formattedDate", style = MaterialTheme.typography.bodySmall)
                Text(bill.amount.formatAsCurrency(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            
            Row {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (bill.state == RecurringState.ACTIVE.name) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
