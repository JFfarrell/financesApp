package com.example.personalfinances.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.util.CurrencyFormatter

/**
 * A single row in the transaction list, used for every [TransactionType].
 *
 * A coloured avatar shows the category's initial (the colour is stable per category name). The
 * category name is the title, followed by the merchant, notes and, for recurring transactions,
 * the cadence, then any tags. The date is not shown because rows sit under a day heading.
 *
 * Expenses show in the normal text colour with a minus sign, income in the income colour with a
 * plus sign, and savings in the savings colour without a sign. A repeat icon marks recurring
 * transactions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val wallet = MaterialTheme.wallet
    val category = wallet.categoryColor(transaction.category.name)

    val amountText = CurrencyFormatter.format(transaction.amount)
    val (amount, amountColor) = when (transaction.transactionType) {
        TransactionType.EXPENSE -> "−$amountText" to wallet.text
        TransactionType.INCOME -> "+$amountText" to wallet.income
        TransactionType.SAVING -> amountText to wallet.saving
    }

    // Merchant, notes and cadence share one line, e.g. "Lidl · weekly shop · Monthly".
    val detail = listOfNotNull(
        transaction.merchant?.name,
        transaction.notes?.takeIf { it.isNotBlank() },
        if (transaction.isRecurring) cadenceLabel(transaction.cadenceUnit, transaction.cadenceValue) else null
    ).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(category.tint),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.category.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = category.onTint
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.category.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                if (transaction.isRecurring) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Recurring",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = wallet.muted
                    )
                }
            }
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = wallet.muted
                )
            }
            if (transaction.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    transaction.tags.sorted().forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = wallet.onTag,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(wallet.tagContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
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
