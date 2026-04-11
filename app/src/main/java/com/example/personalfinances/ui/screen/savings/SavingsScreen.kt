package com.example.personalfinances.ui.screen.savings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.personalfinances.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goal") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SavingsEvent.ShowEditTarget) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit target")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressArc(
                            progressFraction = uiState.goal.progressFraction,
                            size = 200.dp,
                            strokeWidth = 20.dp
                        )
                        Text(
                            text = "${(uiState.goal.progressFraction * 100).toInt()}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Saved: ${CurrencyFormatter.format(uiState.goal.currentSaved)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Goal: ${CurrencyFormatter.format(uiState.goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = { viewModel.onEvent(SavingsEvent.ShowEditSaved) }) {
                        Text("Update Amount Saved")
                    }
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

    if (uiState.isEditingSaved) {
        AmountEditDialog(
            title = "Update Amount Saved",
            currentValue = uiState.goal.currentSaved,
            onDismiss = { viewModel.onEvent(SavingsEvent.HideEditSaved) },
            onConfirm = { value -> viewModel.onEvent(SavingsEvent.UpdateCurrentSaved(value)) }
        )
    }
}

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
                onClick = {
                    text.toDoubleOrNull()?.let { onConfirm(it) }
                },
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
