package com.example.personalfinances.ui.screen.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.ui.component.MonthSelector
import com.example.personalfinances.ui.theme.DarkWalletColors
import com.example.personalfinances.ui.theme.LightWalletColors
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.util.CurrencyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Home screen: what is left this month, where it went, and progress towards the savings goal.
 *
 * Drawn as three cards under the month picker: the summary ([SummaryHero]), the spending donut
 * ([CategoryDonutCard]) and the goal ([SavingsGoalCard]). The settings button opens
 * [SettingsSheet], which holds the appearance choice, the pay-cycle start day, a link to the
 * category and merchant manager ([onOpenManage]), backup, and Log out (which calls [onLogout]).
 * A card at the top reminds the user to back up when there is data and no recent backup.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onLogout: () -> Unit = {},
    onOpenManage: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // The system file picker: no storage permission is needed, and the user chooses where the
    // backup lives (device storage, a cloud drive, an SD card, ...). Shared by the reminder card
    // and the Settings sheet.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.onEvent(DashboardEvent.ExportBackup(uri.toString())) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onEvent(DashboardEvent.RequestImport(uri.toString())) }
    val startExport = { exportLauncher.launch("personal-wallot-backup-${LocalDate.now()}.json") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        HomeHeader(onSettingsClick = { viewModel.onEvent(DashboardEvent.ShowSettingsSheet) })
        MonthSelector(
            selectedMonth = uiState.selectedMonth,
            onPreviousMonth = { viewModel.onEvent(DashboardEvent.PreviousMonth) },
            onNextMonth = { viewModel.onEvent(DashboardEvent.NextMonth) },
            payCycleStartDay = uiState.payCycleStartDay
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val reminder = uiState.backupReminder
                if (reminder != null && !uiState.reminderDismissed) {
                    BackupReminderCard(
                        daysSince = reminder.daysSince,
                        onBackUp = startExport,
                        onLater = { viewModel.onEvent(DashboardEvent.DismissBackupReminder) }
                    )
                }
                // Result of a backup started from the reminder (the Settings sheet shows its own).
                val backupMessage = uiState.backupMessage
                if (backupMessage != null && !uiState.isSettingsSheetOpen) {
                    BackupMessageRow(
                        message = backupMessage,
                        isError = uiState.backupIsError,
                        onDismiss = { viewModel.onEvent(DashboardEvent.DismissBackupMessage) }
                    )
                }
                SummaryHero(
                    remainder = uiState.remainder,
                    spent = uiState.totalSpent,
                    saved = uiState.totalSaved,
                    income = uiState.totalIncome
                )
                CategoryDonutCard(byCategory = uiState.expensesByCategory)
                SavingsGoalCard(
                    target = uiState.savingsTarget,
                    current = uiState.savingsCurrent,
                    savedThisCycle = uiState.totalSaved
                )
            }
        }
    }

    if (uiState.isSettingsSheetOpen) {
        SettingsSheet(
            currentStartDay = uiState.payCycleStartDay,
            themeMode = uiState.themeMode,
            onThemeSelected = { viewModel.onEvent(DashboardEvent.SetThemeMode(it)) },
            onDismiss = { viewModel.onEvent(DashboardEvent.HideSettingsSheet) },
            onSave = { day -> viewModel.onEvent(DashboardEvent.SetPayCycleStartDay(day)) },
            lastBackupAt = uiState.lastBackupAt,
            isBackupBusy = uiState.isBackupBusy,
            backupMessage = uiState.backupMessage,
            backupIsError = uiState.backupIsError,
            onExportClick = startExport,
            onImportClick = { importLauncher.launch(arrayOf("*/*")) },
            onManage = {
                // Close the sheet first, or it would still be open when the user comes back.
                viewModel.onEvent(DashboardEvent.HideSettingsSheet)
                onOpenManage()
            },
            onLogout = onLogout
        )
    }

    if (uiState.pendingImport != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(DashboardEvent.CancelImport) },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Everything in the file is added. Anything that already exists here is replaced " +
                        "by the version in the file. Nothing is deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(DashboardEvent.ConfirmImport) }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(DashboardEvent.CancelImport) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HomeHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Wallot", style = MaterialTheme.typography.headlineSmall)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.wallet.cardTonal)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

/**
 * The headline card: the amount left this month in large type, a bar showing how income was
 * divided between spending, savings and what remains, and the three underlying totals.
 */
@Composable
private fun SummaryHero(remainder: Double, spent: Double, saved: Double, income: Double) {
    val wallet = MaterialTheme.wallet
    // If more went out than came in, the bar shows only spending and savings, in proportion.
    val total = max(income, spent + saved)
    val left = (total - spent - saved).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(wallet.hero)
            .padding(20.dp)
    ) {
        Text(
            text = "Left this month",
            style = MaterialTheme.typography.bodyMedium,
            color = wallet.onHero.copy(alpha = 0.9f)
        )
        Text(
            text = CurrencyFormatter.format(remainder),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold),
            color = wallet.onHero
        )
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (total <= 0.0) {
                Box(Modifier.weight(1f).height(10.dp).background(wallet.heroLeft))
            } else {
                if (spent > 0) Box(Modifier.weight(spent.toFloat()).height(10.dp).background(wallet.heroSpent))
                if (saved > 0) Box(Modifier.weight(saved.toFloat()).height(10.dp).background(wallet.heroSaved))
                if (left > 0) Box(Modifier.weight(left.toFloat()).height(10.dp).background(wallet.heroLeft))
            }
        }
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroStat("Spent", spent, wallet.heroSpent, Modifier.weight(1f))
            HeroStat("Saved", saved, wallet.heroSaved, Modifier.weight(1f))
            HeroStat("Income", income, wallet.onHero, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, amount: Double, dot: Color, modifier: Modifier = Modifier) {
    val onHero = MaterialTheme.wallet.onHero
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text(label, style = MaterialTheme.typography.labelMedium, color = onHero.copy(alpha = 0.85f))
        }
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            color = onHero
        )
    }
}

/**
 * Donut chart of everything that went out this month, split by category, with a legend of the
 * largest categories. Each category keeps the same colour on every screen (see
 * [com.example.personalfinances.ui.theme.WalletColors.categoryColor]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDonutCard(byCategory: Map<String, Double>) {
    val wallet = MaterialTheme.wallet
    val sorted = byCategory.entries.sortedByDescending { it.value }
    val total = sorted.sumOf { it.value }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(wallet.card)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sorted.isEmpty() || total <= 0.0) {
            Text(
                text = "Nothing has gone out this month yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = wallet.muted
            )
            return@Column
        }

        Box(modifier = Modifier.size(176.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 24.dp.toPx()
                var startAngle = -90f
                sorted.forEach { (name, value) ->
                    val sweep = (value / total * 360.0).toFloat()
                    drawArc(
                        color = wallet.categoryColor(name).chart,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(CurrencyFormatter.format(total), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("went out", style = MaterialTheme.typography.labelMedium, color = wallet.muted)
            }
        }

        FlowRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sorted.take(4).forEach { (name, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(wallet.categoryColor(name).chart))
                    Text(name, style = MaterialTheme.typography.labelMedium, color = wallet.muted)
                    Text(
                        text = "${(value / total * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = wallet.text
                    )
                }
            }
            if (sorted.size > 4) {
                Text("+${sorted.size - 4} more", style = MaterialTheme.typography.labelMedium, color = wallet.muted)
            }
        }
    }
}

/** Progress towards the savings goal, or a prompt to set one when there is no target yet. */
@Composable
private fun SavingsGoalCard(target: Double, current: Double, savedThisCycle: Double) {
    val wallet = MaterialTheme.wallet
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(wallet.card)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The title never wraps; the longer note beside it takes what is left and wraps instead.
            Text(
                text = "Savings goal",
                style = MaterialTheme.typography.labelLarge,
                color = wallet.muted,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${CurrencyFormatter.format(savedThisCycle)} saved this cycle",
                style = MaterialTheme.typography.labelMedium,
                color = wallet.muted,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = CurrencyFormatter.format(current),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (target > 0.0) {
                Text(
                    text = "of ${CurrencyFormatter.format(target)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = wallet.muted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        if (target > 0.0) {
            LinearProgressIndicator(
                progress = { (current / target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = wallet.saving,
                trackColor = wallet.cardTonal
            )
        } else {
            Text(
                text = "Set a target on the Savings tab to track progress.",
                style = MaterialTheme.typography.bodySmall,
                color = wallet.muted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * A nudge to back up: shown when there is data but no backup yet, or the last one is old.
 * "Back up now" opens the file picker straight away; "Later" hides it until the next launch.
 */
@Composable
private fun BackupReminderCard(daysSince: Int?, onBackUp: () -> Unit, onLater: () -> Unit) {
    val wallet = MaterialTheme.wallet
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(wallet.navIndicator)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Back up your data",
            style = MaterialTheme.typography.titleSmall,
            color = wallet.onNavIndicator
        )
        Text(
            text = if (daysSince == null) {
                "You haven't made a backup yet. Save a copy so your data survives losing or resetting this phone."
            } else {
                "Your last backup was $daysSince days ago. Save a fresh copy to keep it safe."
            },
            style = MaterialTheme.typography.bodySmall,
            color = wallet.onNavIndicator
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onLater) { Text("Later", color = wallet.onNavIndicator) }
            Button(onClick = onBackUp) { Text("Back up now") }
        }
    }
}

/** One-line result of a backup started from the reminder, with an OK to dismiss it. */
@Composable
private fun BackupMessageRow(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val wallet = MaterialTheme.wallet
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
            color = if (isError) MaterialTheme.colorScheme.error else wallet.text,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDismiss) { Text("OK") }
    }
}

/**
 * Settings bottom sheet. Choosing an appearance applies immediately; the pay-cycle day is applied
 * with Save, which also closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    currentStartDay: Int,
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    lastBackupAt: Long?,
    isBackupBusy: Boolean,
    backupMessage: String?,
    backupIsError: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onManage: () -> Unit,
    onLogout: () -> Unit
) {
    val wallet = MaterialTheme.wallet
    val sheetState = rememberModalBottomSheetState()


    val lastBackupText = lastBackupAt?.let {
        "Last backup: " + Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
    } ?: "You have not made a backup yet."
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(currentStartDay) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = wallet.sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)

            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = wallet.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOptionTile(
                        mode = mode,
                        selected = mode == themeMode,
                        onClick = { onThemeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = "System follows your phone's dark mode setting.",
                style = MaterialTheme.typography.bodySmall,
                color = wallet.muted
            )

            Text(
                text = "Pay cycle",
                style = MaterialTheme.typography.labelLarge,
                color = wallet.muted,
                modifier = Modifier.padding(top = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "Day $selectedDay",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pay cycle starts on") },
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
                    (1..28).forEach { day ->
                        DropdownMenuItem(
                            text = { Text("Day $day") },
                            onClick = {
                                selectedDay = day
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onSave(selectedDay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = onManage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, wallet.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = wallet.text)
            ) { Text("Categories & merchants") }

            Text(
                text = "Backup",
                style = MaterialTheme.typography.labelLarge,
                color = wallet.muted,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(lastBackupText, style = MaterialTheme.typography.bodySmall, color = wallet.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onExportClick,
                    enabled = !isBackupBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(1.dp, wallet.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = wallet.text)
                ) { Text("Export") }
                OutlinedButton(
                    onClick = onImportClick,
                    enabled = !isBackupBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(1.dp, wallet.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = wallet.text)
                ) { Text("Import") }
            }
            if (backupMessage != null) {
                Text(
                    text = backupMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backupIsError) MaterialTheme.colorScheme.error else wallet.muted
                )
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                border = BorderStroke(1.dp, wallet.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = wallet.text)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text("Log out", modifier = Modifier.padding(start = 8.dp))
            }
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
