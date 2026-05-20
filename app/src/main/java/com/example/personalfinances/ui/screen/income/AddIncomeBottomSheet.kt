package com.example.personalfinances.ui.screen.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.personalfinances.domain.model.IncomeType
import com.example.personalfinances.ui.component.TransactionTypeField
import com.example.personalfinances.util.DateUtils

/**
 * Cadence options available when adding an income entry.
 *
 * [months] maps to the [Income.cadenceMonths] field. A value of 0 means one-time;
 * -1 is a sentinel for the "Custom" option which reveals a manual entry field.
 */
private enum class CadenceOption(val label: String, val months: Int) {
    OneTime("One-time", 0),
    Monthly("Monthly", 1),
    Quarterly("Every 3 months", 3),
    BiAnnual("Every 6 months", 6),
    Annual("Yearly", 12),
    Custom("Custom…", -1)
}

/**
 * Modal bottom sheet for adding a new income entry.
 *
 * The sheet collects:
 * - Amount (decimal number)
 * - Type (selected from the fixed [IncomeType] enum via [TransactionTypeField])
 * - Description (mandatory free text, only shown when type is [IncomeType.OTHER])
 * - Cadence (how often the income recurs, or one-time)
 *
 * The Save button is disabled until the amount is non-blank and, if the type is OTHER,
 * the description is also non-blank.
 *
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [IncomeEvent.AddIncome] when the user taps Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeBottomSheet(
    onDismiss: () -> Unit,
    onSave: (IncomeEvent.AddIncome) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<IncomeType?>(null) }
    var description by remember { mutableStateOf("") }
    var cadenceOption by remember { mutableStateOf(CadenceOption.Monthly) }
    var customCadenceText by remember { mutableStateOf("") }
    var cadenceDropdownExpanded by remember { mutableStateOf(false) }

    // The Save button is enabled when:
    //   1. An amount has been entered, AND
    //   2. A type has been selected, AND
    //   3. If the type is OTHER, a description has been provided
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Income", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // TransactionTypeField handles both the type dropdown and the description area.
            // It is generic, so it can be reused identically for expense types in the future.
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
                        IncomeEvent.AddIncome(
                            amount = amount,
                            type = type,
                            description = description.takeIf { it.isNotBlank() },
                            cadenceMonths = cadence,
                            startDate = DateUtils.todayEpochMillis()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text("Save Income")
            }
        }
    }
}
