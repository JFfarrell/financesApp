package com.example.personalfinances.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.theme.ExpenseRed
import com.example.personalfinances.ui.theme.IncomeGreen
import com.example.personalfinances.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
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
