package com.example.personalfinances.ui.screen.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.ui.component.CreatablePicker
import com.example.personalfinances.ui.component.TagInput
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.ui.theme.LocalMoneyFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/** The optional fields that open an inline editor when their chip is tapped. */
private enum class DetailEditor { MERCHANT, NOTES, TAGS, REPEAT }

/**
 * Modal bottom sheet for adding or editing a transaction of any [TransactionType].
 *
 * Built for quick entry, top to bottom in the order things are filled in: a segmented control
 * picks the type, the amount is typed on an on-screen keypad that sits directly beneath it, and
 * the category is one tap on a chip. Everything optional (date, merchant, notes, tags, repeat)
 * lives in a row of compact chips below that; tapping one opens its editor beneath the row, and
 * the date chip opens the date picker. Save is last.
 *
 * When [initialTransaction] is null the sheet is in Add mode and pre-selects [defaultType];
 * otherwise it pre-populates every field. Editing keeps the transaction's id and recurring group.
 *
 * The category chips show only the [categories] of the selected type, with an inline "+ New"
 * option; a new category takes the current type, is passed to [onCreateCategory] and is selected
 * immediately. Changing the type clears a category that no longer applies. The merchant editor
 * works the same way over [merchants]. Typing a name that already exists selects the existing
 * item instead of creating a duplicate. Tags go through [TagInput] and are always lowercase.
 *
 * In Add mode with repeat on, a "For how many months?" field is shown and [onSave] receives the
 * duration so the caller can create the whole series. Only monthly cadence is exposed for now;
 * the model supports other units (see backlog item 13).
 *
 * @param initialTransaction Transaction to edit, or null for Add mode.
 * @param defaultType Type pre-selected in Add mode.
 * @param defaultDate Date pre-filled in Add mode (first day of the selected month).
 * @param categories Categories available to pick from.
 * @param onCreateCategory Called with a newly created category so it can be saved.
 * @param merchants Merchants available to pick from.
 * @param onCreateMerchant Called with a newly created merchant so it can be saved.
 * @param onDismiss Called when the sheet is dismissed without saving.
 * @param onSave Called with the completed [Transaction] and the number of months to create.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    initialTransaction: Transaction? = null,
    defaultType: TransactionType,
    defaultDate: LocalDate,
    categories: List<Category>,
    onCreateCategory: (Category) -> Unit,
    merchants: List<Merchant>,
    onCreateMerchant: (Merchant) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Transaction, durationMonths: Int) -> Unit
) {
    val wallet = MaterialTheme.wallet
    val money = LocalMoneyFormatter.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditMode = initialTransaction != null

    var type by remember { mutableStateOf(initialTransaction?.transactionType ?: defaultType) }
    var amountText by remember { mutableStateOf(initialTransaction?.amount?.let(::formatAmount) ?: "") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var date by remember { mutableStateOf(initialTransaction?.date ?: defaultDate) }
    var selectedCategory by remember { mutableStateOf(initialTransaction?.category) }
    var isCreatingCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedMerchant by remember { mutableStateOf(initialTransaction?.merchant) }
    var tags by remember { mutableStateOf<Set<String>>(initialTransaction?.tags ?: emptySet()) }
    var isRecurring by remember { mutableStateOf(initialTransaction?.isRecurring ?: false) }
    var cadenceText by remember {
        mutableStateOf(initialTransaction?.cadenceValue?.takeIf { it > 0 }?.toString() ?: "1")
    }
    var durationText by remember { mutableStateOf("1") }
    var openEditor by remember { mutableStateOf<DetailEditor?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Material3's DatePicker works in UTC-midnight millis, so convert at the boundary.
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    // Each transaction type has its own categories, so only offer those matching the chosen type.
    val typeCategories = categories.filter { it.type == type }
    val amount = amountText.toDoubleOrNull()
    val isSaveEnabled = amount != null && amount > 0.0 && selectedCategory != null

    fun toggleEditor(editor: DetailEditor) {
        openEditor = if (openEditor == editor) null else editor
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = wallet.sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isEditMode) {
                Text("Edit transaction", style = MaterialTheme.typography.titleMedium)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypeSegments(
                    selected = type,
                    onSelected = { option ->
                        type = option
                        // A category from the previous type no longer applies.
                        if (selectedCategory?.type != option) selectedCategory = null
                        isCreatingCategory = false
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = wallet.cardTonal)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            AmountDisplay(amountText = amountText, symbol = money.symbol)
            Keypad(
                showDecimalPoint = money.fractionDigits > 0,
                onKey = { key -> amountText = applyKey(amountText, key, money.fractionDigits) }
            )

            Text("Category", style = MaterialTheme.typography.labelLarge, color = wallet.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory?.id == category.id,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name, style = MaterialTheme.typography.labelLarge) },
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
                AssistChip(
                    onClick = { isCreatingCategory = true },
                    label = { Text("+ New", style = MaterialTheme.typography.labelLarge) },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, wallet.onNavIndicator),
                    colors = AssistChipDefaults.assistChipColors(labelColor = wallet.onNavIndicator)
                )
            }
            if (isCreatingCategory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New category name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = newCategoryName.isNotBlank(),
                        onClick = {
                            val name = newCategoryName.trim()
                            val existing = typeCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                            if (existing != null) {
                                selectedCategory = existing
                            } else {
                                val created = Category(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    type = type
                                )
                                onCreateCategory(created)
                                selectedCategory = created
                            }
                            newCategoryName = ""
                            isCreatingCategory = false
                        }
                    ) { Text("Add") }
                }
            }

            Text("Details", style = MaterialTheme.typography.labelLarge, color = wallet.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailChip(
                    icon = Icons.Default.CalendarToday,
                    label = if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                    open = false,
                    onClick = { showDatePicker = true }
                )
                DetailChip(
                    icon = Icons.Default.Storefront,
                    label = selectedMerchant?.name ?: "Merchant",
                    open = openEditor == DetailEditor.MERCHANT,
                    onClick = { toggleEditor(DetailEditor.MERCHANT) }
                )
                DetailChip(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = notes.ifBlank { "Notes" },
                    open = openEditor == DetailEditor.NOTES,
                    onClick = { toggleEditor(DetailEditor.NOTES) }
                )
                DetailChip(
                    icon = Icons.Default.Sell,
                    label = when (tags.size) {
                        0 -> "Tags"
                        1 -> "#${tags.first()}"
                        else -> "${tags.size} tags"
                    },
                    open = openEditor == DetailEditor.TAGS,
                    onClick = { toggleEditor(DetailEditor.TAGS) }
                )
                DetailChip(
                    icon = Icons.Default.Repeat,
                    label = if (isRecurring) "Every ${cadenceText.ifBlank { "1" }} mo" else "Once",
                    open = openEditor == DetailEditor.REPEAT,
                    onClick = { toggleEditor(DetailEditor.REPEAT) }
                )
            }

            when (openEditor) {
                DetailEditor.MERCHANT -> CreatablePicker(
                    label = "Merchant",
                    options = merchants,
                    selected = selectedMerchant,
                    optionName = { it.name },
                    newOptionLabel = "+ New merchant",
                    newNameLabel = "New merchant name",
                    noneLabel = "None",
                    onSelected = { selectedMerchant = it },
                    onCreate = { name ->
                        val existing = merchants.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        if (existing != null) {
                            selectedMerchant = existing
                        } else {
                            val created = Merchant(id = UUID.randomUUID().toString(), name = name)
                            onCreateMerchant(created)
                            selectedMerchant = created
                        }
                    }
                )
                DetailEditor.NOTES -> OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                DetailEditor.TAGS -> TagInput(
                    tags = tags,
                    onTagsChange = { tags = it },
                    modifier = Modifier.fillMaxWidth()
                )
                DetailEditor.REPEAT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Repeat monthly", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    }
                    if (isRecurring) {
                        OutlinedTextField(
                            value = cadenceText,
                            onValueChange = { cadenceText = it },
                            label = { Text("Repeat every N months") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isEditMode) {
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { durationText = it },
                                label = { Text("For how many months?") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                null -> Unit
            }

            Button(
                onClick = {
                    val value = amount ?: return@Button
                    val category = selectedCategory ?: return@Button
                    val cadence = if (isRecurring) cadenceText.toIntOrNull()?.coerceAtLeast(1) ?: 1 else 0
                    val duration = if (isRecurring && !isEditMode)
                        durationText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    else 1
                    onSave(
                        Transaction(
                            id = initialTransaction?.id ?: UUID.randomUUID().toString(),
                            transactionType = type,
                            amount = value,
                            date = date,
                            cadenceUnit = CadenceUnit.MONTHS,
                            cadenceValue = cadence,
                            category = category,
                            merchant = selectedMerchant,
                            isRecurring = isRecurring,
                            recurringGroupId = initialTransaction?.recurringGroupId,
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                            tags = tags
                        ),
                        duration
                    )
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (isEditMode) "Save changes" else "Save ${saveNoun(type)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** Noun for the save button: "Save expense", "Save income", "Save savings". */
private fun saveNoun(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "expense"
    TransactionType.INCOME -> "income"
    TransactionType.SAVING -> "savings"
}

/** Formats a stored amount for the keypad text: plain digits, no exponent, no trailing zeros. */
private fun formatAmount(amount: Double): String =
    BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()

/**
 * Applies one keypad press to the amount text: digits append (up to nine whole digits and
 * [maxDecimals] decimals, the currency's usual number), "." starts the decimals once (and is
 * ignored for currencies without decimals), and "⌫" removes the last character.
 */
private fun applyKey(current: String, key: String, maxDecimals: Int): String = when (key) {
    "⌫" -> current.dropLast(1)
    "." -> when {
        maxDecimals == 0 -> current
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    else -> when {
        current.contains('.') && current.substringAfter('.').length >= maxDecimals -> current
        !current.contains('.') && current.length >= 9 -> current
        current == "0" -> key
        else -> current + key
    }
}

/** The typed amount in large type beside the currency [symbol]; shows a muted 0 until typed. */
@Composable
private fun AmountDisplay(amountText: String, symbol: String) {
    val wallet = MaterialTheme.wallet
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Amount", style = MaterialTheme.typography.labelMedium, color = wallet.muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                color = wallet.muted,
                modifier = Modifier.padding(end = 4.dp, bottom = 6.dp)
            )
            Text(
                text = amountText.ifEmpty { "0" },
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp, fontWeight = FontWeight.Bold),
                color = if (amountText.isEmpty()) wallet.muted else wallet.text
            )
        }
    }
}

/** Pill-shaped control for choosing Expense, Income or Saving. */
@Composable
private fun TypeSegments(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val wallet = MaterialTheme.wallet
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(wallet.cardTonal)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransactionType.entries.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (on) wallet.selected else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelected(option) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) wallet.onSelected else wallet.muted
                )
            }
        }
    }
}

/** Compact outlined chip showing one optional detail; highlighted while its editor is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailChip(icon: ImageVector, label: String, open: Boolean, onClick: () -> Unit) {
    val wallet = MaterialTheme.wallet
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 2.dp)
            )
        },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        shape = CircleShape,
        border = BorderStroke(1.dp, if (open) wallet.selected else wallet.outline),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (open) wallet.cardTonal else Color.Transparent,
            labelColor = wallet.text,
            leadingIconContentColor = wallet.muted
        )
    )
}

/**
 * On-screen number pad: digits 0-9, a decimal point and backspace, in a 3 by 4 grid. The decimal
 * point is left blank for currencies that have no decimals.
 */
@Composable
private fun Keypad(showDecimalPoint: Boolean, onKey: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", if (showDecimalPoint) "." else "", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> KeypadKey(key = key, onClick = { onKey(key) }) }
            }
        }
    }
}

@Composable
private fun RowScope.KeypadKey(key: String, onClick: () -> Unit) {
    val wallet = MaterialTheme.wallet
    // A blank key (the decimal point of a currency without decimals) is just an empty gap.
    if (key.isEmpty()) {
        Box(modifier = Modifier.weight(1f).height(50.dp))
        return
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(wallet.cardTonal)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete last digit")
        } else {
            Text(key, style = MaterialTheme.typography.titleLarge)
        }
    }
}
