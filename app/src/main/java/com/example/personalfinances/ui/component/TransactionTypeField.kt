package com.example.personalfinances.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.TransactionType

/**
 * A reusable form field combining a type dropdown with a context-sensitive description area.
 *
 * For types where [TransactionType.isDescriptionEditable] is false, a read-only helper text is
 * shown beneath the dropdown displaying [TransactionType.defaultDescription]. This gives the user
 * context about what the type means without allowing edits.
 *
 * For types where [TransactionType.isDescriptionEditable] is true (i.e. "Other"), an editable
 * [OutlinedTextField] is shown instead, and [onDescriptionChange] is called as the user types.
 *
 * This composable is stateless — the caller owns [selectedType] and [description].
 *
 * @param types The complete list of available type options to show in the dropdown.
 * @param selectedType The currently selected type, or null if none has been chosen yet.
 * @param onTypeSelected Called when the user picks a type from the dropdown.
 * @param description The current description text (relevant only when type is editable).
 * @param onDescriptionChange Called as the user edits the description field.
 * @param modifier Optional [Modifier] for the outer [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : TransactionType> TransactionTypeField(
    types: List<T>,
    selectedType: T?,
    onTypeSelected: (T) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedType?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                placeholder = { Text("Select a type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Show description area only once a type has been selected
        selectedType?.let { type ->
            if (type.isDescriptionEditable) {
                // "Other" — show an editable field the user must fill in
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    placeholder = { Text("Describe this income source") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else {
                // Fixed type — show the built-in description as read-only helper text
                Text(
                    text = type.defaultDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }
        }
    }
}
