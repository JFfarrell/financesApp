package com.example.personalfinances.ui.screen.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.personalfinances.ui.component.ExpenseListItem
import com.example.personalfinances.ui.component.MonthSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyScreen(viewModel: MonthlyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Monthly View") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                onPreviousMonth = { viewModel.onEvent(MonthlyEvent.PreviousMonth) },
                onNextMonth = { viewModel.onEvent(MonthlyEvent.NextMonth) }
            )

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val allEmpty = uiState.expenses.isEmpty() && uiState.recurringExpenses.isEmpty()
                    if (allEmpty) {
                        Text(
                            text = "No expenses for this month.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (uiState.expenses.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "This Month",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(uiState.expenses, key = { it.id }) { expense ->
                                    ExpenseListItem(expense = expense)
                                }
                            }

                            if (uiState.recurringExpenses.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Recurring",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(uiState.recurringExpenses, key = { "r_${it.id}" }) { expense ->
                                    ExpenseListItem(expense = expense)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
