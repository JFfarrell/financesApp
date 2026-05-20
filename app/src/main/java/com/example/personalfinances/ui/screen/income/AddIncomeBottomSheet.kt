package com.example.personalfinances.ui.screen.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.model.IncomeType
import com.example.personalfinances.ui.component.TransactionTypeField

/**
 * Cadence options available when adding or editing an income entry.
 *
 * [months] maps to [Income.cadenceMonths]. A value of 0 means one-time; -1 is a sentinel for
 * the "Custom" option which reveals a manual entry field.
 */
private enum class CadenceOption(val label: String, val months: Int) {
    OneTime("One-time", 0),
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
 * @param onSave Called with the completed [Income] when the user taps Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeBottomSheet(
    initialIncome: Income? = null,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (Income) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isEditMode = initialIncome != null

    var amountText by remember { mutableStateOf(initialIncome?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf<IncomeType?>(initialIncome?.type) }
    var description by remember { mutableStateOf(initialIncome?.description ?: "") }
    var cadenceOption by remember {
        mutableStateOf(
            if (initialIncome != null) cadenceOptionFor(initialIncome.cadenceMonths)
            else CadenceOption.Monthly
        )
    }
    var customCadenceText by remember {
        mutableStateOf(
            if (initialIncome != null && cadenceOptionFor(initialIncome.cadenceMonths) == CadenceOption.Custom)
                initialIncome.cadenceMonths.toString()
            else ""
        )
    }
    var cadenceDropdownExpanded by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    val type = selectedType ?: return@Button
                    val cadence = when {
                        cadenceOption == CadenceOption.Custom -> customCadenceText.toIntOrNull() ?: 1
                        else -> cadenceOption.months
                    }
                    onSave(
                        Income(
                            id = initialIncome?.id ?: 0,
                            amount = amount,
                            type = type,
                            description = description.takeIf { it.isNotBlank() },
                            cadenceMonths = cadence,
                            startDate = initialIncome?.startDate ?: defaultDateMillis
                        )
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
