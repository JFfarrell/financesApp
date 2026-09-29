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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.component.TransactionListItem
import com.example.personalfinances.ui.screen.transaction.AddTransactionBottomSheet
import com.example.personalfinances.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val defaultDate = DateUtils.monthDateRange(uiState.selectedMonth, uiState.payCycleStartDay).first

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
                    onClick = {
                        viewModel.onEvent(CalendarEvent.ShowAddTransactionSheet(TransactionType.EXPENSE))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Expense") }

                OutlinedButton(
                    onClick = {
                        viewModel.onEvent(CalendarEvent.ShowAddTransactionSheet(TransactionType.INCOME))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Income") }

                OutlinedButton(
                    onClick = {
                        viewModel.onEvent(CalendarEvent.ShowAddTransactionSheet(TransactionType.SAVING))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Savings") }
            }

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    uiState.transactions.isEmpty() && uiState.recurringTransactions.isEmpty() ->
                        Text(
                            text = "No transactions for this month.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // One section per transaction type: one-off entries first, then a
                            // "Recurring" sub-section.
                            TransactionType.entries.forEach { type ->
                                val oneOff = uiState.transactions.filter { it.transactionType == type }
                                val recurring = uiState.recurringTransactions.filter { it.transactionType == type }
                                if (oneOff.isNotEmpty() || recurring.isNotEmpty()) {
                                    item(key = "header_${type.name}") {
                                        SectionHeader(sectionTitle(type))
                                    }
                                    items(oneOff, key = { it.id }) { transaction ->
                                        TransactionRow(transaction, viewModel::onEvent)
                                    }
                                    if (recurring.isNotEmpty()) {
                                        item(key = "recurring_${type.name}") {
                                            SubSectionHeader("Recurring")
                                        }
                                        items(recurring, key = { it.id }) { transaction ->
                                            TransactionRow(transaction, viewModel::onEvent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isTransactionSheetOpen) {
        AddTransactionBottomSheet(
            initialTransaction = uiState.transactionSheetTarget,
            defaultType = uiState.sheetDefaultType,
            defaultDate = defaultDate,
            categories = uiState.categories,
            onCreateCategory = { viewModel.onEvent(CalendarEvent.AddCategory(it)) },
            onDismiss = { viewModel.onEvent(CalendarEvent.HideTransactionSheet) },
            onSave = { transaction, durationMonths ->
                val event = if (uiState.transactionSheetTarget == null)
                    CalendarEvent.AddTransaction(transaction, durationMonths)
                else
                    CalendarEvent.UpdateTransaction(transaction)
                viewModel.onEvent(event)
            }
        )
    }

    when (val dialog = uiState.recurringDialog) {
        is RecurringDialogState.PendingDelete ->
            RecurringActionDialog(
                title = "Delete recurring transaction",
                onConfirm = { scope ->
                    viewModel.onEvent(CalendarEvent.ConfirmDelete(dialog.transaction, scope))
                },
                onDismiss = { viewModel.onEvent(CalendarEvent.DismissRecurringDialog) }
            )
        is RecurringDialogState.PendingUpdate ->
            RecurringActionDialog(
                title = "Edit recurring transaction",
                onConfirm = { scope ->
                    viewModel.onEvent(CalendarEvent.ConfirmUpdate(dialog.updated, scope))
                },
                onDismiss = { viewModel.onEvent(CalendarEvent.DismissRecurringDialog) }
            )
        RecurringDialogState.None -> Unit
    }
}

/** Section title shown above each transaction type's entries. */
private fun sectionTitle(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "Expenses"
    TransactionType.INCOME -> "Income"
    TransactionType.SAVING -> "Savings"
}

/** A transaction row with swipe-to-delete; tapping opens the edit sheet. */
@Composable
private fun TransactionRow(
    transaction: Transaction,
    onEvent: (CalendarEvent) -> Unit
) {
    SwipeToDeleteBox(onDelete = { onEvent(CalendarEvent.DeleteTransaction(transaction)) }) {
        TransactionListItem(
            transaction = transaction,
            onClick = { onEvent(CalendarEvent.ShowEditTransactionSheet(transaction)) }
        )
    }
}

/**
 * Dialog shown when the user tries to delete or edit a recurring transaction.
 * The user must choose whether the change applies to this entry only or to this
 * and all future entries in the series.
 */
@Composable
private fun RecurringActionDialog(
    title: String,
    onConfirm: (RecurringScope) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("Apply this change to just this entry, or to this and all future entries in the series?") },
        confirmButton = {
            TextButton(onClick = { onConfirm(RecurringScope.THIS_AND_FUTURE) }) {
                Text("This & future")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(RecurringScope.THIS_ONLY) }) {
                Text("This only")
            }
        }
    )
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

/**
 * Wraps [content] in a swipe-to-dismiss gesture. Swiping left reveals a red delete background
 * and calls [onDelete]; the item then snaps back (confirmValueChange returns false) so the UI
 * remains stable while the ViewModel decides whether to delete immediately or show a dialog.
 */
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
            }
            false
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
