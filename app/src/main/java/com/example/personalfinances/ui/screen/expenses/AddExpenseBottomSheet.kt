package com.example.personalfinances.ui.screen.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.ExpenseCategory
import com.example.personalfinances.domain.model.ExpenseType
import com.example.personalfinances.ui.component.HierarchicalTypeField
import com.example.personalfinances.util.DateUtils

/**
 * Modal bottom sheet for adding a new expense entry.
 *
 * The sheet collects:
 * - Amount (decimal number, required)
 * - Title (short label for this specific entry, required)
 * - Category + Type (two-step picker via [HierarchicalTypeField], both required)
 * - Description (optional note; required when type is an `*_OTHER` variant)
 * - Recurring toggle + cadence
 *
 * The Save button is disabled until amount, title, and type are all provided, and — for
 * `*_OTHER` types — a description has also been entered.
 *
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [ExpensesEvent.AddExpense] when the user taps Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    onDismiss: () -> Unit,
    onSave: (ExpensesEvent.AddExpense) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selectedType by remember { mutableStateOf<ExpenseType?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var cadenceText by remember { mutableStateOf("1") }

    // Description is only required for *_OTHER types; title is always required.
    val needsDescription = selectedType?.isDescriptionEditable == true
    val isSaveEnabled = amountText.isNotBlank()
        && title.isNotBlank()
        && selectedType != null
        && (!needsDescription || description.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Expense", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Tesco, Monthly gym, Shell") },
                modifier = Modifier.fillMaxWidth()
            )

            // Two-step hierarchical type picker: category → subtype.
            // The type picker shows the built-in defaultDescription as helper text below the
            // subtype dropdown. The separate description field below is always shown and required.
            HierarchicalTypeField(
                categories = ExpenseCategory.entries,
                categoryDisplayName = { it.displayName },
                subtypesForCategory = { category ->
                    ExpenseType.entries.filter { it.category == category }
                },
                selectedCategory = selectedCategory,
                selectedType = selectedType,
                onCategorySelected = { category ->
                    selectedCategory = category
                    selectedType = null
                },
                onTypeSelected = { selectedType = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (needsDescription) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe this expense") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring expense")
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            if (isRecurring) {
                OutlinedTextField(
                    value = cadenceText,
                    onValueChange = { cadenceText = it },
                    label = { Text("Repeat every N months") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    val type = selectedType ?: return@Button
                    val cadence = if (isRecurring) cadenceText.toIntOrNull() ?: 1 else 0
                    onSave(
                        ExpensesEvent.AddExpense(
                            amount = amount,
                            title = title,
                            description = description,
                            type = type,
                            date = DateUtils.todayEpochMillis(),
                            isRecurring = isRecurring,
                            cadenceMonths = cadence
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text("Save Expense")
            }
        }
    }
}
