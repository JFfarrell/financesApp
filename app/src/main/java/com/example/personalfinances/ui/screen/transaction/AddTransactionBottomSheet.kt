package com.example.personalfinances.ui.screen.transaction

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Modal bottom sheet for adding or editing a transaction of any [TransactionType].
 *
 * When [initialTransaction] is null the sheet is in Add mode and pre-selects [defaultType];
 * otherwise it pre-populates every field and switches labels to "Edit". Editing keeps the
 * transaction's id, recurring group, merchant and tags, so fields this sheet does not yet expose
 * are never lost.
 *
 * The category picker lists the [categories] belonging to the selected transaction type and
 * offers an inline "New category" option. A newly created category takes the current type, is
 * passed to [onCreateCategory] and is selected immediately. Changing the type clears a category
 * that no longer applies.
 *
 * In Add mode with recurring enabled, a "For how many months?" field is shown and [onSave]
 * receives the duration so the caller can create the whole series. Only monthly cadence is
 * exposed for now; the model supports other units (see backlog item 13).
 *
 * @param initialTransaction Transaction to edit, or null for Add mode.
 * @param defaultType Type pre-selected in Add mode.
 * @param defaultDate Date pre-filled in Add mode (first day of the selected month).
 * @param categories Categories available in the picker.
 * @param onCreateCategory Called with a newly created category so it can be saved.
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [Transaction] and the number of months to create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    initialTransaction: Transaction? = null,
    defaultType: TransactionType,
    defaultDate: LocalDate,
    categories: List<Category>,
    onCreateCategory: (Category) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Transaction, durationMonths: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val isEditMode = initialTransaction != null

    var type by remember { mutableStateOf(initialTransaction?.transactionType ?: defaultType) }
    var amountText by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var date by remember { mutableStateOf(initialTransaction?.date ?: defaultDate) }
    var selectedCategory by remember { mutableStateOf(initialTransaction?.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isCreatingCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var cadenceText by remember {
        mutableStateOf(initialTransaction?.cadenceValue?.takeIf { it > 0 }?.toString() ?: "1")
    }
    var durationText by remember { mutableStateOf("1") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Material3's DatePicker works in UTC-midnight millis, so convert at the boundary.
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    // Each transaction type has its own categories, so only offer those matching the chosen type.
    val typeCategories = categories.filter { it.type == type }

    val isSaveEnabled = amountText.toDoubleOrNull() != null && selectedCategory != null

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                if (isEditMode) "Edit Transaction" else "Add Transaction",
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = {
                            type = option
                            // A category from the previous type no longer applies.
                            if (selectedCategory?.type != option) selectedCategory = null
                            isCreatingCategory = false
                        },
                        label = { Text(option.displayName) }
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    typeCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                isCreatingCategory = false
                                categoryExpanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ New category") },
                        onClick = {
                            isCreatingCategory = true
                            categoryExpanded = false
                        }
                    )
                }
            }

            if (isCreatingCategory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New category name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = newCategoryName.isNotBlank(),
                        onClick = {
                            val created = Category(
                                id = UUID.randomUUID().toString(),
                                name = newCategoryName.trim(),
                                type = type
                            )
                            onCreateCategory(created)
                            selectedCategory = created
                            newCategoryName = ""
                            isCreatingCategory = false
                        }
                    ) { Text("Add") }
                }
            }

            OutlinedTextField(
                value = date.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring")
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
                    val category = selectedCategory ?: return@Button
                    val cadence = if (isRecurring) cadenceText.toIntOrNull()?.coerceAtLeast(1) ?: 1 else 0
                    val duration = if (isRecurring && !isEditMode)
                        durationText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    else 1
                    onSave(
                        Transaction(
                            id = initialTransaction?.id ?: UUID.randomUUID().toString(),
                            transactionType = type,
                            amount = amount,
                            date = date,
                            cadenceUnit = CadenceUnit.MONTHS,
                            cadenceValue = cadence,
                            category = category,
                            merchant = initialTransaction?.merchant,
                            isRecurring = isRecurring,
                            recurringGroupId = initialTransaction?.recurringGroupId,
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                            tags = initialTransaction?.tags ?: emptySet()
                        ),
                        duration
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            ) {
                Text(if (isEditMode) "Save Changes" else "Save Transaction")
            }
        }
    }
}
