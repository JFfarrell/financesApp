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
import com.example.personalfinances.util.DateUtils

private enum class CadenceOption(val label: String, val months: Int) {
    OneTime("One-time", 0),
    Monthly("Monthly", 1),
    Quarterly("Every 3 months", 3),
    BiAnnual("Every 6 months", 6),
    Annual("Yearly", 12),
    Custom("Custom…", -1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeBottomSheet(
    onDismiss: () -> Unit,
    onSave: (IncomeEvent.AddIncome) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var cadenceOption by remember { mutableStateOf(CadenceOption.Monthly) }
    var customCadenceText by remember { mutableStateOf("") }
    var cadenceDropdownExpanded by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Source (e.g. Salary, Freelance)") },
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cadenceDropdownExpanded) },
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
                    val cadence = when {
                        cadenceOption == CadenceOption.Custom -> customCadenceText.toIntOrNull() ?: 1
                        else -> cadenceOption.months
                    }
                    onSave(
                        IncomeEvent.AddIncome(
                            amount = amount,
                            source = source,
                            cadenceMonths = cadence,
                            startDate = DateUtils.todayEpochMillis()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountText.isNotBlank() && source.isNotBlank()
            ) {
                Text("Save Income")
            }
        }
    }
}
