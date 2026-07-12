package com.example.personalfinances.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.theme.ExpenseRed
import com.example.personalfinances.ui.theme.IncomeGreen
import com.example.personalfinances.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(DashboardEvent.ShowSettingsSheet) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                onPreviousMonth = { viewModel.onEvent(DashboardEvent.PreviousMonth) },
                onNextMonth = { viewModel.onEvent(DashboardEvent.NextMonth) }
            )
            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SummaryCard(
                                totalIncome = uiState.totalIncome,
                                totalExpenses = uiState.totalExpenses,
                                remainder = uiState.remainder
                            )
                        }
                        if (uiState.expensesByCategory.isNotEmpty()) {
                            item {
                                Text(
                                    "Breakdown by Category",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            items(uiState.expensesByCategory.entries.toList()) { (category, amount) ->
                                CategoryBreakdownRow(category = category, amount = amount)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isSettingsSheetOpen) {
        PayCycleSettingsSheet(
            currentStartDay = uiState.payCycleStartDay,
            onDismiss = { viewModel.onEvent(DashboardEvent.HideSettingsSheet) },
            onSave = { day -> viewModel.onEvent(DashboardEvent.SetPayCycleStartDay(day)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayCycleSettingsSheet(
    currentStartDay: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(currentStartDay) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Pay Cycle Settings", style = MaterialTheme.typography.titleLarge)

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "Day $selectedDay",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pay cycle starts on") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    (1..28).forEach { day ->
                        DropdownMenuItem(
                            text = { Text("Day $day") },
                            onClick = {
                                selectedDay = day
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onSave(selectedDay) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalIncome: Double,
    totalExpenses: Double,
    remainder: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow(label = "Total Income", amount = totalIncome, color = IncomeGreen)
            SummaryRow(label = "Total Expenses", amount = totalExpenses, color = ExpenseRed)
            HorizontalDivider()
            SummaryRow(
                label = "Remainder",
                amount = remainder,
                color = if (remainder >= 0) IncomeGreen else ExpenseRed
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

@Composable
private fun CategoryBreakdownRow(category: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(category, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.bodyLarge,
            color = ExpenseRed
        )
    }
    HorizontalDivider()
}
