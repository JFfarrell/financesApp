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
 * A two-step type picker for hierarchical transaction types.
 *
 * Step 1 — Category dropdown: shows all entries in [categories], using [categoryDisplayName]
 * to render each label. Selecting a category clears any previously selected subtype.
 *
 * Step 2 — Subtype dropdown: only appears after a category is chosen. Populated with
 * [subtypesForCategory](selectedCategory), which should return only the subtypes that belong
 * to that category. Once a subtype is selected, its [TransactionType.defaultDescription] is
 * shown below the dropdown as read-only helper text (empty for `*_OTHER` types).
 *
 * This composable is stateless — the caller owns [selectedCategory] and [selectedType]. Any
 * additional description input (e.g. for `*_OTHER` types) should be handled by the caller as
 * a separate field below this composable.
 *
 * @param C The category enum type (e.g. [com.example.personalfinances.domain.model.ExpenseCategory]).
 * @param T The subtype enum type implementing [TransactionType].
 * @param categories All available top-level categories.
 * @param categoryDisplayName Extracts the display label from a category value.
 * @param subtypesForCategory Returns the list of subtypes that belong to a given category.
 * @param selectedCategory The currently selected category, or null.
 * @param selectedType The currently selected subtype, or null.
 * @param onCategorySelected Called when the user picks a category; the caller should also
 *   clear [selectedType] when this fires.
 * @param onTypeSelected Called when the user picks a subtype.
 * @param modifier Optional [Modifier] for the outer [Column].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <C, T : TransactionType> HierarchicalTypeField(
    categories: List<C>,
    categoryDisplayName: (C) -> String,
    subtypesForCategory: (C) -> List<T>,
    selectedCategory: C?,
    selectedType: T?,
    onCategorySelected: (C) -> Unit,
    onTypeSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {

        // Step 1 — Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory?.let(categoryDisplayName) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                placeholder = { Text("Select a category") },
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
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(categoryDisplayName(category)) },
                        onClick = {
                            onCategorySelected(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Step 2 — Subtype dropdown (only shown once a category is selected)
        selectedCategory?.let { category ->
            val subtypes = subtypesForCategory(category)

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedType?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    placeholder = { Text("Select a type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    subtypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onTypeSelected(type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Helper text — shown once a subtype is selected (empty for *_OTHER types)
            selectedType?.let { type ->
                if (type.defaultDescription.isNotEmpty()) {
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
}
