package com.example.personalfinances.ui.screen.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.ui.component.ExpenseListItem

/**
 * Root composable for the Expenses screen.
 *
 * Observes [ExpensesViewModel.uiState] and delegates rendering to child composables.
 * User interactions are forwarded to [ExpensesViewModel.onEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expenses") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(ExpensesEvent.ShowAddSheet) }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
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
                uiState.expenses.isEmpty() -> Text(
                    text = "No expenses yet. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseListItem(expense = expense)
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetVisible) {
        AddExpenseBottomSheet(
            onDismiss = { viewModel.onEvent(ExpensesEvent.HideAddSheet) },
            onSave = { event -> viewModel.onEvent(event) }
        )
    }
}
