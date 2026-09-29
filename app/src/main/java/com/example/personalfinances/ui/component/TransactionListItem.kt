package com.example.personalfinances.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.theme.ExpenseRed
import com.example.personalfinances.ui.theme.IncomeGreen
import com.example.personalfinances.util.CurrencyFormatter
import java.time.format.DateTimeFormatter

/**
 * A single row in the transaction list, used for every [TransactionType].
 *
 * The category name is the primary label, followed by the merchant and notes when present, then
 * the date (and cadence, for recurring transactions) and any tags. The amount is coloured by
 * type: red for expenses, green for income, and the theme's primary colour for savings.
 *
 * A repeat icon is shown next to the title when [Transaction.isRecurring] is true.
 */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.SAVING -> MaterialTheme.colorScheme.primary
    }

    // Merchant and notes share one line, e.g. "Woolworths - weekly shop".
    val detail = listOfNotNull(
        transaction.merchant?.name,
        transaction.notes?.takeIf { it.isNotBlank() }
    ).joinToString(" - ")

    val dateText = transaction.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    val dateLine = if (transaction.isRecurring) {
        "$dateText - ${cadenceLabel(transaction.cadenceUnit, transaction.cadenceValue)}"
    } else {
        dateText
    }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (transaction.isRecurring) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.tags.isNotEmpty()) {
                    Text(
                        text = transaction.tags.sorted().joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = CurrencyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** Human-readable cadence, e.g. "Monthly" or "Every 2 weeks". */
private fun cadenceLabel(unit: CadenceUnit, value: Int): String {
    if (value <= 1) {
        return when (unit) {
            CadenceUnit.DAYS -> "Daily"
            CadenceUnit.WEEKS -> "Weekly"
            CadenceUnit.MONTHS -> "Monthly"
            CadenceUnit.YEARS -> "Yearly"
        }
    }
    return "Every $value ${unit.displayName.lowercase()}"
}
