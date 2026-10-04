package com.example.personalfinances.ui.screen.monthly

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.component.TransactionListItem
import com.example.personalfinances.ui.screen.transaction.AddTransactionBottomSheet
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.ui.theme.LocalMoneyFormatter
import com.example.personalfinances.util.MoneyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** Which transactions the list shows; [type] null means all of them. */
private enum class TransactionFilter(val label: String, val type: TransactionType?) {
    ALL("All", null),
    EXPENSES("Expenses", TransactionType.EXPENSE),
    INCOME("Income", TransactionType.INCOME),
    SAVINGS("Savings", TransactionType.SAVING)
}

/**
 * Transactions screen: the selected month's transactions grouped by day, newest first, with
 * filter chips for the type. Tapping a row edits it and swiping left deletes it. The add button
 * in the top right opens the add sheet.
 */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableStateOf(TransactionFilter.ALL) }
    val money = LocalMoneyFormatter.current

    // After a delete, offer Undo for a while. A new delete replaces the previous prompt (the effect
    // restarts with the new token and the old snackbar is dismissed), so only the latest can be undone.
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingUndo = uiState.undoDelete
    LaunchedEffect(pendingUndo?.token) {
        if (pendingUndo != null) {
            val result = snackbarHostState.showSnackbar(
                message = pendingUndo.message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(CalendarEvent.UndoLastDelete)
            } else {
                viewModel.onEvent(CalendarEvent.ClearUndo)
            }
        }
    }

    // One-off and recurring transactions are shown together, grouped by day.
    val byDay = (uiState.transactions + uiState.recurringTransactions)
        .filter { filter.type == null || it.transactionType == filter.type }
        .sortedByDescending { it.date }
        .groupBy { it.date }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Same height as the Home header so the month pill does not jump when switching tabs.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Transactions", style = MaterialTheme.typography.headlineSmall)
            IconButton(
                onClick = {
                    viewModel.onEvent(CalendarEvent.ShowAddTransactionSheet(filter.type ?: TransactionType.EXPENSE))
                },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.wallet.addButton,
                    contentColor = MaterialTheme.wallet.onAddButton
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
        MonthSelector(
            selectedMonth = uiState.selectedMonth,
            onPreviousMonth = { viewModel.onEvent(CalendarEvent.PreviousMonth) },
            onNextMonth = { viewModel.onEvent(CalendarEvent.NextMonth) },
            payCycleStartDay = uiState.payCycleStartDay
        )
        FilterRow(selected = filter, onSelected = { filter = it })

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                byDay.isEmpty() -> Text(
                    text = if (filter == TransactionFilter.ALL) "No transactions for this month."
                    else "No ${filter.label.lowercase()} for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wallet.muted,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    byDay.forEach { (date, dayItems) ->
                        item(key = "header_$date") {
                            DayHeader(
                                label = date.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                                total = dayTotal(dayItems, money)
                            )
                        }
                        item(key = "group_$date") {
                            DayCard(items = dayItems, onEvent = viewModel::onEvent)
                        }
                    }
                    item { Box(Modifier.height(8.dp)) }
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
    )
    }

    if (uiState.isTransactionSheetOpen) {
        AddTransactionBottomSheet(
            initialTransaction = uiState.transactionSheetTarget,
            defaultType = uiState.sheetDefaultType,
            defaultDate = LocalDate.now(),
            categories = uiState.categories,
            onCreateCategory = { viewModel.onEvent(CalendarEvent.AddCategory(it)) },
            merchants = uiState.merchants,
            frequentMerchants = uiState.frequentMerchants,
            onCreateMerchant = { viewModel.onEvent(CalendarEvent.AddMerchant(it)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(selected: TransactionFilter, onSelected: (TransactionFilter) -> Unit) {
    val wallet = MaterialTheme.wallet
    // Scrolls sideways when the chips are wider than the screen, so a chip is never squeezed
    // (which made its label wrap onto a second line on narrower phones).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = wallet.cardTonal,
                    labelColor = wallet.text,
                    selectedContainerColor = wallet.selected,
                    selectedLabelColor = wallet.onSelected
                )
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, total: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.wallet.muted)
        Text(total, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.wallet.muted)
    }
}

/**
 * A rounded card holding one day's transactions. Rows are separated by a hairline and each
 * supports swipe-to-delete.
 */
@Composable
private fun DayCard(items: List<Transaction>, onEvent: (CalendarEvent) -> Unit) {
    val wallet = MaterialTheme.wallet
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(wallet.card)
    ) {
        items.forEachIndexed { index, transaction ->
            SwipeToDeleteBox(onDelete = { onEvent(CalendarEvent.DeleteTransaction(transaction)) }) {
                TransactionListItem(
                    transaction = transaction,
                    onClick = { onEvent(CalendarEvent.ShowEditTransactionSheet(transaction)) }
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = wallet.cardTonal
                )
            }
        }
    }
}

/**
 * The figure shown at the right of a day heading. Days with any income or expenses show their
 * net (income minus expenses, signed); a day with only savings shows the amount saved.
 */
private fun dayTotal(items: List<Transaction>, money: MoneyFormatter): String {
    val hasCash = items.any { it.transactionType != TransactionType.SAVING }
    if (!hasCash) return money.format(items.sumOf { it.amount })

    val net = items.sumOf {
        when (it.transactionType) {
            TransactionType.INCOME -> it.amount
            TransactionType.EXPENSE -> -it.amount
            TransactionType.SAVING -> 0.0
        }
    }
    val sign = if (net < 0) "−" else if (net > 0) "+" else ""
    return sign + money.format(abs(net))
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
        Box(modifier = Modifier.background(MaterialTheme.wallet.card)) {
            content()
        }
    }
}
