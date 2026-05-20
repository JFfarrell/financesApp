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
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.model.IncomeType
import com.example.personalfinances.ui.theme.IncomeGreen
import com.example.personalfinances.util.CurrencyFormatter

/**
 * A single row in the income list showing type name, description, cadence, and amount.
 *
 * The displayed description is the user-provided text for [IncomeType.OTHER], or the type's
 * built-in [defaultDescription] for all other types.
 */
@Composable
fun IncomeListItem(
    income: Income,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val descriptionText = income.description?.takeIf { it.isNotBlank() }
        ?: income.type.defaultDescription

    val cadenceLabel = when (income.cadenceMonths) {
        0 -> "One-time"
        1 -> "Monthly"
        else -> "Every ${income.cadenceMonths} months"
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
                    Text(income.type.displayName, style = MaterialTheme.typography.bodyLarge)
                    if (income.cadenceMonths > 0) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            modifier = Modifier.padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
