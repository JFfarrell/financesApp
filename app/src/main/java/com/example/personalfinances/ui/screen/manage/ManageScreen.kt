package com.example.personalfinances.ui.screen.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.theme.wallet

private enum class ManageTab(val label: String) {
    CATEGORIES("Categories"),
    MERCHANTS("Merchants")
}

/**
 * Screen for renaming and deleting categories and merchants. Each row shows how many transactions
 * use the item. Renaming updates every transaction that uses it, because transactions point at
 * the item rather than copying its name. An item that is in use cannot be deleted.
 */
@Composable
fun ManageScreen(
    onBack: () -> Unit,
    viewModel: ManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(ManageTab.CATEGORIES) }
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
                    if (uiState.merchants.isEmpty()) {
                        item { EmptyNote("No merchants yet. Add one from the transaction sheet.") }
                    } else {
                        item {
                            GroupCard {
                                uiState.merchants.forEachIndexed { index, managed ->
                                    val target = ManageTarget.OfMerchant(managed)
                                    ManageRow(
                                        name = managed.merchant.name,
                                        usage = managed.usage,
                                        onRename = { viewModel.onEvent(ManageEvent.StartRename(target)) },
                                        onDelete = { viewModel.onEvent(ManageEvent.StartDelete(target)) }
                                    )
                                    if (index < uiState.merchants.lastIndex) RowDivider()
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
    val wallet = MaterialTheme.wallet
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ManageTab.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(option.label, style = MaterialTheme.typography.labelLarge) },
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
private fun ManageRow(name: String, usage: Int, onRename: () -> Unit, onDelete: () -> Unit) {
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
                },
                style = MaterialTheme.typography.bodySmall,
                color = wallet.muted
            )
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
