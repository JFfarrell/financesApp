package com.example.personalfinances.ui.screen.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.ui.theme.IncomeGreen
import com.example.personalfinances.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(IncomeEvent.ShowAddSheet) }) {
                Icon(Icons.Default.Add, contentDescription = "Add income")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.incomes.isEmpty() -> Text(
                    text = "No income entries yet. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val oneTime = uiState.incomes.filter { it.cadenceMonths == 0 }
                    val recurring = uiState.incomes.filter { it.cadenceMonths > 0 }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (recurring.isNotEmpty()) {
                            item {
                                Text(
                                    "Recurring",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(recurring, key = { it.id }) { income ->
                                IncomeListItem(income = income)
                            }
                        }
                        if (oneTime.isNotEmpty()) {
                            item {
                                Text(
                                    "One-time",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(oneTime, key = { "ot_${it.id}" }) { income ->
                                IncomeListItem(income = income)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetVisible) {
        AddIncomeBottomSheet(
            onDismiss = { viewModel.onEvent(IncomeEvent.HideAddSheet) },
            onSave = { event -> viewModel.onEvent(event) }
        )
    }
}

@Composable
private fun IncomeListItem(income: Income) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(income.source, style = MaterialTheme.typography.bodyLarge)
                    if (income.cadenceMonths > 0) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                val cadenceLabel = when (income.cadenceMonths) {
                    0 -> "One-time"
                    1 -> "Monthly"
                    else -> "Every ${income.cadenceMonths} months"
                }
                Text(
                    text = cadenceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.format(income.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = IncomeGreen
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
