package com.example.personalfinances.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.personalfinances.domain.model.normalizeTag

/**
 * Lets the user build a set of tags one at a time.
 *
 * Existing [tags] are shown as removable chips. New tags are typed into a text field and added
 * with the Done key, the Add button, or by typing a space or comma. Every entry goes through
 * [normalizeTag], so tags are always lowercase with no spaces, and adding a tag that is already
 * present has no effect because [tags] is a set.
 *
 * @param tags The current tags.
 * @param onTagsChange Called with the full updated set whenever a tag is added or removed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagInput(
    tags: Set<String>,
    onTagsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    fun commit(raw: String) {
        normalizeTag(raw)?.let { onTagsChange(tags + it) }
        text = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.sorted().forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag $tag",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onTagsChange(tags - tag) }
                            )
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    // A space or comma finishes the current tag, since tags cannot contain spaces.
                    if (value.endsWith(" ") || value.endsWith(",")) commit(value) else text = value
                },
                label = { Text("Add a tag") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit(text) }),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { commit(text) }
            ) { Text("Add") }
        }
    }
}
