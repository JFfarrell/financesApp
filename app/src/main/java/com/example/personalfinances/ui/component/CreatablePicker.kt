package com.example.personalfinances.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A dropdown that lists [options], optionally lets the user clear the choice, and lets them create
 * a new option inline.
 *
 * Choosing "[newOptionLabel]" reveals a name field with an Add button; pressing Add calls
 * [onCreate] with the typed name. The caller builds the new item, saves it, and updates
 * [selected]; the picker holds no data of its own beyond whether the name field is showing.
 *
 * If [noneLabel] is set, an extra entry with that label is shown first and selecting it calls
 * [onSelected] with null, for optional fields such as merchant.
 *
 * @param label Text shown as the field label.
 * @param options Items the user can pick from.
 * @param selected Currently selected item, or null when nothing is chosen.
 * @param optionName Maps an item to the text displayed for it.
 * @param newOptionLabel Text of the "create new" menu entry, e.g. "+ New category".
 * @param newNameLabel Label of the name field shown while creating.
 * @param onSelected Called with the chosen item, or null when "None" is chosen.
 * @param onCreate Called with the trimmed name the user typed to create a new item.
 * @param noneLabel Label for the clear-selection entry, or null to hide it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CreatablePicker(
    label: String,
    options: List<T>,
    selected: T?,
    optionName: (T) -> String,
    newOptionLabel: String,
    newNameLabel: String,
    onSelected: (T?) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected?.let(optionName) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (noneLabel != null) {
                    DropdownMenuItem(
                        text = { Text(noneLabel) },
                        onClick = {
                            onSelected(null)
                            isCreating = false
                            expanded = false
                        }
                    )
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionName(option)) },
                        onClick = {
                            onSelected(option)
                            isCreating = false
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(newOptionLabel) },
                    onClick = {
                        isCreating = true
                        expanded = false
                    }
                )
            }
        }

        if (isCreating) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(newNameLabel) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        onCreate(newName.trim())
                        newName = ""
                        isCreating = false
                    }
                ) { Text("Add") }
            }
        }
    }
}
