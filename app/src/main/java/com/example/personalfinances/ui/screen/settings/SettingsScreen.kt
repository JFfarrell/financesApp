package com.example.personalfinances.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.ui.theme.DarkWalletColors
import com.example.personalfinances.ui.theme.LightWalletColors
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.util.Currencies
import com.example.personalfinances.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which small picker dialog, if any, is open on the Settings screen. */
private enum class SettingsDialog { NONE, CURRENCY, PAY_CYCLE }

/**
 * Settings screen, laid out as grouped cards so related choices sit together:
 *  - General: currency and the day the pay cycle starts (each opens a small picker).
 *  - Appearance: System, Light or Dark.
 *  - Data: the category and merchant manager, and backup export and import.
 *  - Account: log out.
 *
 * Choices are saved as soon as they are made. Importing asks for confirmation first, because it
 * can replace records that already exist.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManage: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val settings by viewModel.uiState.collectAsState()
    val backup by backupViewModel.uiState.collectAsState()
    val wallet = MaterialTheme.wallet
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.NONE) }

    // The system file picker: no storage permission is needed, and the user chooses where the
    // backup lives (device storage, a cloud drive, an SD card, ...).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) backupViewModel.onEvent(BackupEvent.Export(uri.toString())) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backupViewModel.onEvent(BackupEvent.RequestImport(uri.toString())) }

    val lastBackupText = backup.lastBackupAt?.let {
        "Last backup: " + Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
    } ?: "You have not made a backup yet"

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
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection("General") {
                SettingsRow(
                    title = "Currency",
                    subtitle = Currencies.describe(settings.currencyCode),
                    onClick = { dialog = SettingsDialog.CURRENCY }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Pay cycle starts on",
                    subtitle = "Day ${settings.payCycleStartDay} of each month",
                    onClick = { dialog = SettingsDialog.PAY_CYCLE }
                )
            }

            SettingsSection("Appearance") {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeOptionTile(
                                mode = mode,
                                selected = mode == settings.themeMode,
                                onClick = { viewModel.onEvent(SettingsEvent.SetThemeMode(mode)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "System follows your phone's dark mode setting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = wallet.muted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            SettingsSection("Data") {
                SettingsRow(
                    title = "Categories & merchants",
                    subtitle = "Rename or delete",
                    onClick = onManage
                )
                SettingsDivider()
                SettingsRow(
                    title = "Export backup",
                    subtitle = lastBackupText,
                    enabled = !backup.isBusy,
                    onClick = { exportLauncher.launch("personal-wallot-backup-${LocalDate.now()}.json") }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Import backup",
                    subtitle = "Add data from a backup file",
                    enabled = !backup.isBusy,
                    onClick = { importLauncher.launch(arrayOf("*/*")) }
                )
                backup.message?.let { message ->
                    SettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backup.isError) MaterialTheme.colorScheme.error else wallet.text,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { backupViewModel.onEvent(BackupEvent.DismissMessage) }) {
                            Text("OK")
                        }
                    }
                }
            }

            SettingsSection("Account") {
                SettingsRow(title = "Log out", showChevron = false, onClick = onLogout)
            }
        }
    }

    when (dialog) {
        SettingsDialog.CURRENCY -> CurrencyPickerDialog(
            selectedCode = settings.currencyCode,
            onSelect = { code ->
                viewModel.onEvent(SettingsEvent.SetCurrency(code))
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE }
        )
        SettingsDialog.PAY_CYCLE -> PayCycleDialog(
            selectedDay = settings.payCycleStartDay,
            onSelect = { day ->
                viewModel.onEvent(SettingsEvent.SetPayCycleStartDay(day))
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE }
        )
        SettingsDialog.NONE -> Unit
    }

    if (backup.pendingImport != null) {
        AlertDialog(
            onDismissRequest = { backupViewModel.onEvent(BackupEvent.CancelImport) },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Everything in the file is added. Anything that already exists here is replaced " +
                        "by the version in the file. Nothing is deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = { backupViewModel.onEvent(BackupEvent.ConfirmImport) }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { backupViewModel.onEvent(BackupEvent.CancelImport) }) { Text("Cancel") }
            }
        )
    }
}

/** A titled group of settings drawn as one rounded card. */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val wallet = MaterialTheme.wallet
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = wallet.muted,
            modifier = Modifier.padding(start = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(wallet.card)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.wallet.cardTonal)
}

/** One tappable line in a settings card: a title, an optional smaller line beneath, and a chevron. */
@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true
) {
    val wallet = MaterialTheme.wallet
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) wallet.text else wallet.muted
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = wallet.muted)
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = wallet.muted
            )
        }
    }
}

/**
 * One choice in the appearance picker: a small preview of the theme, its name, and a border and
 * tick when selected. The preview always shows the theme's own colours, whichever theme is
 * active now; System is split diagonally between light and dark.
 */
@Composable
private fun ThemeOptionTile(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallet = MaterialTheme.wallet
    val light = LightWalletColors.background
    val dark = DarkWalletColors.background
    val preview: Brush = when (mode) {
        ThemeMode.LIGHT -> SolidColor(light)
        ThemeMode.DARK -> SolidColor(dark)
        ThemeMode.SYSTEM -> Brush.linearGradient(
            0f to light, 0.5f to light, 0.5f to dark, 1f to dark
        )
    }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else wallet.outline), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(preview)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = wallet.onNavIndicator,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(mode.displayName, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Searchable list of currencies, with "Phone default" first. Choosing one closes the dialog and
 * saves it. Only how amounts are shown changes; existing amounts are never converted.
 */
@Composable
private fun CurrencyPickerDialog(
    selectedCode: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val wallet = MaterialTheme.wallet
    val all = remember { Currencies.all() }
    val deviceCode = remember { MoneyFormatter.deviceCurrency().currencyCode }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) all else all.filter {
            it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = wallet.sheet,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "Only changes how amounts are shown. Existing amounts are not converted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = wallet.muted,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search by name or code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (query.isBlank()) {
                        item {
                            CurrencyRow(
                                title = "Phone default",
                                subtitle = deviceCode,
                                selected = selectedCode == null,
                                onClick = { onSelect(null) }
                            )
                        }
                    }
                    items(filtered, key = { it.code }) { option ->
                        CurrencyRow(
                            title = option.name,
                            subtitle = "${option.code}  ${option.symbol}",
                            selected = option.code == selectedCode,
                            onClick = { onSelect(option.code) }
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 12.dp, bottom = 8.dp)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun CurrencyRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val wallet = MaterialTheme.wallet
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = wallet.muted)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = wallet.onNavIndicator)
        }
    }
}

/** A grid of the days 1 to 28; tapping one saves it as the day the pay cycle starts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayCycleDialog(selectedDay: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val wallet = MaterialTheme.wallet
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay cycle starts on") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The day of the month your pay period begins. Home and Transactions group each month from this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = wallet.muted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..28).forEach { day ->
                        val on = day == selectedDay
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (on) wallet.selected else wallet.cardTonal)
                                .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(day) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) wallet.onSelected else wallet.text
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
