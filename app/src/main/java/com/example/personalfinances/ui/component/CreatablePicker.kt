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
 * With [searchable] the field becomes a type-to-filter box, which suits long lists: opening it
 * clears the text so the user can start typing at once, the options narrow to those containing
 * what was typed (ignoring case), and when the text matches no existing option the menu offers
 * `+ Add "text"`, so creating takes the same single step as choosing. The separate "new" entry and
 * name field are not used in this mode, and closing the menu without choosing restores the
 * current selection's name.
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
 * @param searchable Whether to filter the options as the user types and create from the typed text.
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
    noneLabel: String? = null,
    searchable: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Searchable mode only: the text in the box, and whether it is a search (as opposed to just
    // showing the current selection's name). A new selection resets the text to its name.
    val selectedName = selected?.let(optionName) ?: ""
    var query by remember(selected) { mutableStateOf(selectedName) }
    var isFiltering by remember(selected) { mutableStateOf(false) }
    val typed = query.trim()
    val visibleOptions =
        if (searchable && isFiltering) options.filter { optionName(it).contains(typed, ignoreCase = true) }
        else options
    val canCreateFromQuery = searchable && isFiltering && typed.isNotEmpty() &&
        options.none { optionName(it).equals(typed, ignoreCase = true) }

    fun closeMenu() {
        expanded = false
        query = selectedName
        isFiltering = false
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { open ->
                if (open) {
                    expanded = true
                    if (searchable) query = ""
                } else {
                    closeMenu()
                }
            }
        ) {
            OutlinedTextField(
                value = if (searchable) query else selectedName,
                onValueChange = {
                    if (searchable) {
                        query = it
                        isFiltering = true
                        expanded = true
                    }
                },
                readOnly = !searchable,
                placeholder = if (searchable) {
                    { Text("Search or type a new name") }
                } else null,
                singleLine = searchable,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { closeMenu() }
            ) {
                if (noneLabel != null && !isFiltering) {
                    DropdownMenuItem(
                        text = { Text(noneLabel) },
                        onClick = {
                            onSelected(null)
                            isCreating = false
                            closeMenu()
                        }
                    )
                }
                visibleOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionName(option)) },
                        onClick = {
                            onSelected(option)
                            isCreating = false
                            closeMenu()
                        }
                    )
                }
                if (searchable) {
                    if (canCreateFromQuery) {
                        DropdownMenuItem(
                            text = { Text("+ Add \"$typed\"") },
                            onClick = {
                                onCreate(typed)
                                closeMenu()
                            }
                        )
                    }
                } else {
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
