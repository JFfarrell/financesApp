package com.example.personalfinances.ui.screen.savings

import androidx.compose.foundation.layout.Arrangement
import com.example.personalfinances.ui.theme.wallet
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.personalfinances.ui.component.CircularProgressArc
import com.example.personalfinances.ui.theme.LocalMoneyFormatter

/**
 * Root composable for the Savings screen.
 *
 * Displays a circular progress arc showing progress toward the savings goal. The "Saved" total
 * is computed automatically from the starting amount + all SAVING transactions — it cannot be
 * edited directly. The user can set a "Starting Amount" (pre-app savings seed) and a goal target.
 *
 * When the user adds a savings transaction anywhere in the app, this screen updates automatically
 * because both data sources are combined reactively in [SavingsViewModel].
 */
@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val wallet = MaterialTheme.wallet
    val money = LocalMoneyFormatter.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Savings", style = MaterialTheme.typography.headlineSmall)
            IconButton(
                onClick = { viewModel.onEvent(SavingsEvent.ShowEditTarget) },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = wallet.cardTonal)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit target")
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(wallet.card)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // progressFraction takes currentSaved as a parameter because the value
                        // is computed at runtime, not stored on the model.
                        CircularProgressArc(
                            progressFraction = uiState.goal.progressFraction(uiState.currentSaved),
                            size = 200.dp,
                            strokeWidth = 20.dp
                        )
                        Text(
                            text = "${(uiState.goal.progressFraction(uiState.currentSaved) * 100).toInt()}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = wallet.saving
                        )
                    }

                    Text(
                        text = "Saved: ${money.format(uiState.currentSaved)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Goal: ${money.format(uiState.goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = wallet.muted
                    )
                    Text(
                        text = "Starting amount: ${money.format(uiState.goal.startingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = wallet.muted
                    )
                }

                // Sets the pre-app savings seed value: savings that existed before using the app.
                // The saved total itself is computed, never typed in.
                Button(
                    onClick = { viewModel.onEvent(SavingsEvent.ShowEditStartingAmount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Set starting amount")
                }
            }
        }
    }

    if (uiState.isEditingTarget) {
        AmountEditDialog(
            title = "Set Savings Goal",
            currentValue = uiState.goal.targetAmount,
            onDismiss = { viewModel.onEvent(SavingsEvent.HideEditTarget) },
            onConfirm = { value -> viewModel.onEvent(SavingsEvent.UpdateTarget(value)) }
        )
    }

    if (uiState.isEditingStartingAmount) {
        AmountEditDialog(
            title = "Set Starting Amount",
            currentValue = uiState.goal.startingAmount,
            onDismiss = { viewModel.onEvent(SavingsEvent.HideEditStartingAmount) },
            onConfirm = { value -> viewModel.onEvent(SavingsEvent.UpdateStartingAmount(value)) }
        )
    }
}

/**
 * Generic dialog for editing a single numeric amount.
 *
 * [currentValue] pre-fills the text field. The confirm button is disabled until the input
 * parses as a valid [Double].
 */
@Composable
private fun AmountEditDialog(
    title: String,
    currentValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(if (currentValue > 0) currentValue.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Amount") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { text.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = text.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
