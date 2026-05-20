package com.example.personalfinances.ui.screen.income

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.model.IncomeType
import com.example.personalfinances.ui.component.TransactionTypeField

/**
 * Cadence options shown when the recurring toggle is on.
 *
 * [months] maps to [Income.cadenceMonths]. -1 is a sentinel for the "Custom" option which reveals
 * a manual entry field. OneTime is handled by the [isRecurring] toggle, not this dropdown.
 */
private enum class CadenceOption(val label: String, val months: Int) {
    Monthly("Monthly", 1),
    Quarterly("Every 3 months", 3),
    BiAnnual("Every 6 months", 6),
    Annual("Yearly", 12),
    Custom("Custom…", -1)
}

private fun cadenceOptionFor(months: Int): CadenceOption =
    CadenceOption.entries.firstOrNull { it.months == months && it != CadenceOption.Custom }
        ?: CadenceOption.Custom

/**
 * Modal bottom sheet for adding or editing an income entry.
 *
 * When [initialIncome] is null the sheet is in Add mode; when non-null it pre-populates all
 * fields and switches the title and save button label to "Edit".
 *
 * [defaultDateMillis] is used as [Income.startDate] in Add mode only; in Edit mode the original
 * start date is preserved.
 *
 * @param initialIncome Pre-populated income for Edit mode, or null for Add mode.
 * @param defaultDateMillis Epoch millis for the first day of the currently selected month.
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [Income] and the number of months to create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeBottomSheet(
    initialIncome: Income? = null,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (Income, durationMonths: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isEditMode = initialIncome != null

    var amountText by remember { mutableStateOf(initialIncome?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf<IncomeType?>(initialIncome?.type) }
    var description by remember { mutableStateOf(initialIncome?.description ?: "") }
    var isRecurring by remember { mutableStateOf(initialIncome?.isRecurring ?: false) }
    var cadenceOption by remember {
        mutableStateOf(
            if (initialIncome != null && initialIncome.isRecurring)
                cadenceOptionFor(initialIncome.cadenceMonths)
            else
                CadenceOption.Monthly
        )
    }
    var customCadenceText by remember {
        mutableStateOf(
            if (initialIncome != null && initialIncome.isRecurring
                && cadenceOptionFor(initialIncome.cadenceMonths) == CadenceOption.Custom)
                initialIncome.cadenceMonths.toString()
            else ""
        )
    }
    var cadenceDropdownExpanded by remember { mutableStateOf(false) }
    var durationText by remember { mutableStateOf("1") }

    val isSaveEnabled = amountText.isNotBlank()
        && selectedType != null
        && (selectedType != IncomeType.OTHER || description.isNotBlank())

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
                if (isEditMode) "Edit Income" else "Add Income",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            TransactionTypeField(
                types = IncomeType.entries,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it; description = "" },
                description = description,
                onDescriptionChange = { description = it },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring income")
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            if (isRecurring) {
                ExposedDropdownMenuBox(
                    expanded = cadenceDropdownExpanded,
                    onExpandedChange = { cadenceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = cadenceOption.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cadence") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = cadenceDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = cadenceDropdownExpanded,
                        onDismissRequest = { cadenceDropdownExpanded = false }
                    ) {
                        CadenceOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    cadenceOption = option
                                    cadenceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (cadenceOption == CadenceOption.Custom) {
                    OutlinedTextField(
                        value = customCadenceText,
                        onValueChange = { customCadenceText = it },
                        label = { Text("Every N months") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    val cadence = when {
                        !isRecurring -> 0
                        cadenceOption == CadenceOption.Custom -> customCadenceText.toIntOrNull() ?: 1
                        else -> cadenceOption.months
                    }
                    val duration = if (isRecurring && !isEditMode)
                        durationText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    else 1
                    onSave(
                        Income(
                            id = initialIncome?.id ?: 0,
                            amount = amount,
                            type = type,
                            description = description.takeIf { it.isNotBlank() },
                            isRecurring = isRecurring,
                            cadenceMonths = cadence,
                            startDate = initialIncome?.startDate ?: defaultDateMillis,
                            recurringGroupId = initialIncome?.recurringGroupId
                        ),
                        duration
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text(if (isEditMode) "Save Changes" else "Save Income")
            }
        }
    }
}
