package com.example.personalfinances.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.personalfinances.ui.theme.wallet
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
 * With [searchable] the dropdown is replaced by a type-to-filter box with the matches listed
 * inline right under it (a popup would cover the field, and the keyboard would hide the rest, on a
 * long list). Focusing the box clears the text so the user can start typing at once and shows every
 * option; typing narrows them to those containing the text (ignoring case); and when the text
 * matches no existing option, `+ Add "text"` heads the list, so creating takes the same single
 * step as choosing. [newOptionLabel] and [newNameLabel] are not used in this mode. Leaving the box
 * without choosing restores the current selection's name.
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
    if (searchable) {
        SearchablePicker(
            label = label,
            options = options,
            selected = selected,
            optionName = optionName,
            noneLabel = noneLabel,
            onSelected = onSelected,
            onCreate = onCreate,
            modifier = modifier
        )
        return
    }

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

/**
 * The [CreatablePicker] body for `searchable = true`: a text box with its matching options listed
 * inline below it (see [CreatablePicker]). The list is capped in height and scrolls inside itself,
 * and the whole block is scrolled into view as the user types so it clears the keyboard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> SearchablePicker(
    label: String,
    options: List<T>,
    selected: T?,
    optionName: (T) -> String,
    noneLabel: String?,
    onSelected: (T?) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier
) {
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val selectedName = selected?.let(optionName) ?: ""

    // The text in the box, whether it is a search (rather than just the selection's name), and
    // whether the box has focus (which is when the list shows). A new selection resets the text.
    var query by remember(selected) { mutableStateOf(selectedName) }
    var isFiltering by remember(selected) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    val typed = query.trim()
    val matches =
        if (isFiltering) options.filter { optionName(it).contains(typed, ignoreCase = true) } else options
    val canCreate = isFiltering && typed.isNotEmpty() &&
        options.none { optionName(it).equals(typed, ignoreCase = true) }

    LaunchedEffect(query, focused) {
        if (focused) bringIntoView.bringIntoView()
    }

    Column(
        modifier = modifier.bringIntoViewRequester(bringIntoView),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                isFiltering = true
            },
            label = { Text(label) },
            placeholder = { Text("Search or type a new name") },
            singleLine = true,
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = ""; isFiltering = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear text")
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        focused = true
                        query = ""
                        isFiltering = false
                    } else if (focused) {
                        focused = false
                        query = selectedName
                        isFiltering = false
                    }
                }
        )

        if (focused) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.wallet.cardTonal)
                    .verticalScroll(rememberScrollState())
            ) {
                if (canCreate) {
                    PickerRow(text = "+ Add \"$typed\"", emphasised = true) {
                        onCreate(typed)
                        focusManager.clearFocus()
                    }
                }
                if (noneLabel != null && !isFiltering && selected != null) {
                    PickerRow(text = noneLabel) {
                        onSelected(null)
                        focusManager.clearFocus()
                    }
                }
                matches.forEach { option ->
                    PickerRow(text = optionName(option), emphasised = option == selected) {
                        onSelected(option)
                        focusManager.clearFocus()
                    }
                }
                if (matches.isEmpty() && !canCreate) {
                    PickerRow(text = "No matches", onClick = null)
                }
            }
        }
    }
}

/** One tappable line of the inline list; [emphasised] marks the selection or the create action. */
@Composable
private fun PickerRow(text: String, emphasised: Boolean = false, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasised) FontWeight.Bold else null,
            color = if (onClick == null) MaterialTheme.wallet.muted else MaterialTheme.wallet.text
        )
    }
}
