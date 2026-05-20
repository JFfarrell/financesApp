package com.example.personalfinances.ui.screen.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.ExpenseCategory
import com.example.personalfinances.domain.model.ExpenseType
import com.example.personalfinances.ui.component.HierarchicalTypeField

/**
 * Modal bottom sheet for adding or editing an expense entry.
 *
 * When [initialExpense] is null the sheet is in Add mode; when non-null it pre-populates all
 * fields and switches the title and save button label to "Edit".
 *
 * In Add mode with recurring enabled, a "For how many months?" field is shown; [onSave] receives
 * the duration so the caller can create the full series. In Edit mode the duration is always 1.
 *
 * @param initialExpense Pre-populated expense for Edit mode, or null for Add mode.
 * @param defaultDateMillis Epoch millis for the first day of the currently selected month.
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [Expense] and the number of months to create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    initialExpense: Expense? = null,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (Expense, durationMonths: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isEditMode = initialExpense != null

    var amountText by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialExpense?.type?.category) }
    var selectedType by remember { mutableStateOf<ExpenseType?>(initialExpense?.type) }
    var isRecurring by remember { mutableStateOf(initialExpense?.isRecurring ?: false) }
    var cadenceText by remember { mutableStateOf(
        initialExpense?.cadenceMonths?.takeIf { it > 0 }?.toString() ?: "1"
    ) }
    var durationText by remember { mutableStateOf("1") }

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
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isEditMode) "Edit Expense" else "Add Expense",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )

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
                if (!isEditMode) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("For how many months?") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    val type = selectedType ?: return@Button
                    val cadence = if (isRecurring) cadenceText.toIntOrNull() ?: 1 else 0
                    val duration = if (isRecurring && !isEditMode)
                        durationText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    else 1
                    onSave(
                        Expense(
                            id = initialExpense?.id ?: 0,
                            amount = amount,
                            title = title,
                            description = description,
                            type = type,
                            date = initialExpense?.date ?: defaultDateMillis,
                            isRecurring = isRecurring,
                            cadenceMonths = cadence,
                            recurringGroupId = initialExpense?.recurringGroupId
                        ),
                        duration
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text(if (isEditMode) "Save Changes" else "Save Expense")
            }
        }
    }
}
