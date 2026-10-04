package com.example.personalfinances.ui.screen.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.theme.wallet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ManageTab(val label: String) {
    CATEGORIES("Categories"),
    MERCHANTS("Merchants")
}

/**
 * Screen for renaming and deleting categories and merchants. Each row shows how many transactions
 * use the item. Renaming updates every transaction that uses it, because transactions point at
 * the item rather than copying its name. An item that is in use cannot be deleted.
 *
 * The Merchants tab grows with use, so it has a search box, a sort order (A–Z, most used, most
 * recently used) and an "Unused" filter with a one-tap clean-up. A merchant can also be merged into
 * another: its transactions move over and it is deleted, which is how duplicates are tidied.
 */
@Composable
fun ManageScreen(
    onBack: () -> Unit,
    viewModel: ManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(ManageTab.CATEGORIES) }
    var merchantQuery by rememberSaveable { mutableStateOf("") }
    var merchantSort by rememberSaveable { mutableStateOf(MerchantSort.NAME) }
    var unusedOnly by rememberSaveable { mutableStateOf(false) }
    val shownMerchants = remember(uiState.merchants, merchantQuery, merchantSort, unusedOnly) {
        filterAndSortMerchants(uiState.merchants, merchantQuery, merchantSort, unusedOnly)
    }
    val unusedCount = uiState.merchants.count { it.usage == 0 }
    val wallet = MaterialTheme.wallet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = wallet.cardTonal)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Categories & merchants", style = MaterialTheme.typography.titleLarge)
        }

        TabChips(selected = tab, onSelected = { tab = it })

        uiState.message?.let { message ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(wallet.card)
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.onEvent(ManageEvent.DismissMessage) }) { Text("OK") }
            }
        }

        if (tab == ManageTab.MERCHANTS && uiState.merchants.isNotEmpty()) {
            MerchantSearchField(query = merchantQuery, onQueryChange = { merchantQuery = it })
            MerchantSortRow(
                sort = merchantSort,
                onSortSelected = { merchantSort = it },
                unusedOnly = unusedOnly,
                onUnusedOnlyChange = { unusedOnly = it }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (tab) {
                ManageTab.CATEGORIES -> {
                    TransactionType.entries.forEach { type ->
                        val items = uiState.categories.filter { it.category.type == type }
                        if (items.isNotEmpty()) {
                            item(key = "header_${type.name}") {
                                SectionLabel("${type.displayName} categories")
                            }
                            item(key = "group_${type.name}") {
                                GroupCard {
                                    items.forEachIndexed { index, managed ->
                                        val target = ManageTarget.OfCategory(managed)
                                        ManageRow(
                                            name = managed.category.name,
                                            usage = managed.usage,
                                            onRename = { viewModel.onEvent(ManageEvent.StartRename(target)) },
                                            onDelete = { viewModel.onEvent(ManageEvent.StartDelete(target)) }
                                        )
                                        if (index < items.lastIndex) RowDivider()
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.categories.isEmpty()) {
                        item { EmptyNote("No categories yet.") }
                    }
                }
                ManageTab.MERCHANTS -> {
                    when {
                        uiState.merchants.isEmpty() ->
                            item { EmptyNote("No merchants yet. Add one from the transaction sheet.") }
                        shownMerchants.isEmpty() ->
                            item { EmptyNote(if (unusedOnly && merchantQuery.isBlank()) "Every merchant is in use." else "No merchants match.") }
                        else -> {
                            if (unusedOnly && unusedCount > 0) {
                                item {
                                    TextButton(
                                        onClick = { viewModel.onEvent(ManageEvent.StartDeleteUnused) },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) { Text("Delete all $unusedCount unused") }
                                }
                            }
                            item {
                                GroupCard {
                                    shownMerchants.forEachIndexed { index, managed ->
                                        val target = ManageTarget.OfMerchant(managed)
                                        ManageRow(
                                            name = managed.merchant.name,
                                            usage = managed.usage,
                                            lastUsed = managed.lastUsed,
                                            onRename = { viewModel.onEvent(ManageEvent.StartRename(target)) },
                                            onDelete = { viewModel.onEvent(ManageEvent.StartDelete(target)) },
                                            onMerge = if (uiState.merchants.size > 1) {
                                                { viewModel.onEvent(ManageEvent.StartMerge(managed)) }
                                            } else null
                                        )
                                        if (index < shownMerchants.lastIndex) RowDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Row(Modifier.height(16.dp)) {} }
        }
    }

    uiState.renaming?.let { target ->
        RenameDialog(
            target = target,
            error = uiState.renameError,
            onConfirm = { viewModel.onEvent(ManageEvent.ConfirmRename(it)) },
            onDismiss = { viewModel.onEvent(ManageEvent.CancelRename) }
        )
    }
    uiState.merging?.let { source ->
        MergeDialog(
            source = source,
            candidates = uiState.merchants.filter { it.merchant.id != source.merchant.id },
            onConfirm = { viewModel.onEvent(ManageEvent.ConfirmMerge(it)) },
            onDismiss = { viewModel.onEvent(ManageEvent.CancelMerge) }
        )
    }
    if (uiState.confirmingDeleteUnused) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ManageEvent.CancelDeleteUnused) },
            title = { Text("Delete $unusedCount unused merchants?") },
            text = { Text("Only merchants that no transaction uses are deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ManageEvent.ConfirmDeleteUnused) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ManageEvent.CancelDeleteUnused) }) { Text("Cancel") }
            }
        )
    }
    uiState.deleting?.let { target ->
        DeleteDialog(
            target = target,
            onConfirm = { viewModel.onEvent(ManageEvent.ConfirmDelete) },
            onDismiss = { viewModel.onEvent(ManageEvent.CancelDelete) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabChips(selected: ManageTab, onSelected: (ManageTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ManageTab.entries.forEach { option ->
            PillChip(label = option.label, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

/** The screen's standard selectable pill: tonal when off, the selected colours when on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val wallet = MaterialTheme.wallet
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false) },
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = wallet.cardTonal,
            labelColor = wallet.text,
            selectedContainerColor = wallet.selected,
            selectedLabelColor = wallet.onSelected
        )
    )
}

/** Search box for the merchants list, with a clear button once something is typed. */
@Composable
private fun MerchantSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search merchants") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

/** Sort-order chips plus an "Unused" toggle; scrolls sideways if the chips do not fit. */
@Composable
private fun MerchantSortRow(
    sort: MerchantSort,
    onSortSelected: (MerchantSort) -> Unit,
    unusedOnly: Boolean,
    onUnusedOnlyChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MerchantSort.entries.forEach { option ->
            PillChip(label = option.label, selected = option == sort, onClick = { onSortSelected(option) })
        }
        PillChip(label = "Unused", selected = unusedOnly, onClick = { onUnusedOnlyChange(!unusedOnly) })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.wallet.muted,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.wallet.muted,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.wallet.card)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.wallet.cardTonal)
}

@Composable
private fun ManageRow(
    name: String,
    usage: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    lastUsed: LocalDate? = null,
    onMerge: (() -> Unit)? = null
) {
    val wallet = MaterialTheme.wallet
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when (usage) {
                    0 -> "Not used"
                    1 -> "Used by 1 transaction"
                    else -> "Used by $usage transactions"
                } + (lastUsed?.let { " · last ${it.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = wallet.muted
            )
        }
        if (onMerge != null) {
            IconButton(onClick = onMerge) {
                Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = "Merge $name into another")
            }
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "Rename $name")
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete $name",
                tint = if (usage == 0) wallet.text else wallet.muted
            )
        }
    }
}

/**
 * Asks which merchant [source] should be merged into: a searchable list of the [candidates] (every
 * other merchant). Once one is chosen it spells out what will happen, because a merge can't be
 * undone: the transactions move across and [source] is deleted.
 */
@Composable
private fun MergeDialog(
    source: ManagedMerchant,
    candidates: List<ManagedMerchant>,
    onConfirm: (Merchant) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember(source) { mutableStateOf("") }
    var chosen by remember(source) { mutableStateOf<ManagedMerchant?>(null) }
    val matches = remember(candidates, query) {
        filterAndSortMerchants(candidates, query, MerchantSort.NAME, unusedOnly = false)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge \"${source.merchant.name}\" into…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search merchants") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(matches, key = { it.merchant.id }) { option ->
                        val isChosen = chosen?.merchant?.id == option.merchant.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = isChosen, role = Role.RadioButton, onClick = { chosen = option })
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isChosen, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(option.merchant.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (matches.isEmpty()) {
                    Text("No merchants match.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.wallet.muted)
                }
                chosen?.let { target ->
                    val noun = if (source.usage == 1) "transaction" else "transactions"
                    Text(
                        text = "Moves ${source.usage} $noun to \"${target.merchant.name}\" and deletes " +
                            "\"${source.merchant.name}\". This can't be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wallet.muted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = chosen != null, onClick = { chosen?.let { onConfirm(it.merchant) } }) {
                Text("Merge")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Asks for a new name; stays open and shows the reason if the name is refused. */
@Composable
private fun RenameDialog(
    target: ManageTarget,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(target) { mutableStateOf(target.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Name") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Confirms a delete, or explains why an item in use cannot be deleted. */
@Composable
private fun DeleteDialog(target: ManageTarget, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (target.usage > 0) {
        val noun = if (target.usage == 1) "transaction" else "transactions"
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Can't delete \"${target.name}\"") },
            text = {
                Text("It is used by ${target.usage} $noun. Rename it instead, or move those transactions to another one first.")
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("It isn't used by any transaction. This can't be undone.") },
            confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}
