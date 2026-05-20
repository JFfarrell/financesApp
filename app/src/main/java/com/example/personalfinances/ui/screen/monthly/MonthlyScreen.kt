package com.example.personalfinances.ui.screen.monthly

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.ui.component.ExpenseListItem
import com.example.personalfinances.ui.component.IncomeListItem
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.screen.expenses.AddExpenseBottomSheet
import com.example.personalfinances.ui.screen.income.AddIncomeBottomSheet
import com.example.personalfinances.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val defaultDateMillis = DateUtils.monthBounds(uiState.selectedMonth).first

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendar") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                onPreviousMonth = { viewModel.onEvent(CalendarEvent.PreviousMonth) },
                onNextMonth = { viewModel.onEvent(CalendarEvent.NextMonth) }
            )

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(CalendarEvent.ShowAddExpenseSheet) },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Add Expense") }

                OutlinedButton(
                    onClick = { viewModel.onEvent(CalendarEvent.ShowAddIncomeSheet) },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Add Income") }
            }

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    uiState.expenses.isEmpty() && uiState.recurringExpenses.isEmpty() && uiState.incomes.isEmpty() ->
                        Text(
                            text = "No transactions for this month.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (uiState.expenses.isNotEmpty() || uiState.recurringExpenses.isNotEmpty()) {
                                item {
                                    SectionHeader("Expenses")
                                }
                                items(uiState.expenses, key = { "e_${it.id}" }) { expense ->
                                    SwipeToDeleteBox(onDelete = {
                                        viewModel.onEvent(CalendarEvent.DeleteExpense(expense))
                                    }) {
                                        ExpenseListItem(
                                            expense = expense,
                                            onClick = { viewModel.onEvent(CalendarEvent.ShowEditExpenseSheet(expense)) }
                                        )
                                    }
                                }
                                if (uiState.recurringExpenses.isNotEmpty()) {
                                    item { SubSectionHeader("Recurring") }
                                    items(uiState.recurringExpenses, key = { "er_${it.id}" }) { expense ->
                                        SwipeToDeleteBox(onDelete = {
                                            viewModel.onEvent(CalendarEvent.DeleteExpense(expense))
                                        }) {
                                            ExpenseListItem(
                                                expense = expense,
                                                onClick = { viewModel.onEvent(CalendarEvent.ShowEditExpenseSheet(expense)) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.incomes.isNotEmpty()) {
                                item { SectionHeader("Income") }
                                items(uiState.incomes, key = { "i_${it.id}" }) { income ->
                                    SwipeToDeleteBox(onDelete = {
                                        viewModel.onEvent(CalendarEvent.DeleteIncome(income))
                                    }) {
                                        IncomeListItem(
                                            income = income,
                                            onClick = { viewModel.onEvent(CalendarEvent.ShowEditIncomeSheet(income)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isExpenseSheetOpen) {
        AddExpenseBottomSheet(
            initialExpense = uiState.expenseSheetTarget,
            defaultDateMillis = defaultDateMillis,
            onDismiss = { viewModel.onEvent(CalendarEvent.HideExpenseSheet) },
            onSave = { expense ->
                val event = if (uiState.expenseSheetTarget == null)
                    CalendarEvent.AddExpense(expense)
                else
                    CalendarEvent.UpdateExpense(expense)
                viewModel.onEvent(event)
            }
        )
    }

    if (uiState.isIncomeSheetOpen) {
        AddIncomeBottomSheet(
            initialIncome = uiState.incomeSheetTarget,
            defaultDateMillis = defaultDateMillis,
            onDismiss = { viewModel.onEvent(CalendarEvent.HideIncomeSheet) },
            onSave = { income ->
                val event = if (uiState.incomeSheetTarget == null)
                    CalendarEvent.AddIncome(income)
                else
                    CalendarEvent.UpdateIncome(income)
                viewModel.onEvent(event)
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SubSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            content()
        }
    }
}
