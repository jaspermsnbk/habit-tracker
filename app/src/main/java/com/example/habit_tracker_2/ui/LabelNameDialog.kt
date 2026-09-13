package com.example.habit_tracker_2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal const val NEW_LABEL_DESCRIPTION = "Labels group habits by type, like Fitness or Learning."

/**
 * Dialog for naming a label, whether new or renamed. Blocks blank names and any of
 * [existingNames] (ignoring case); when renaming, leave the label's own name out of those.
 */
@Composable
fun LabelNameDialog(
    title: String,
    confirmText: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
    initialName: String = "",
    description: String? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val duplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(16.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Label name") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) {
                        { Text("A label with this name already exists") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && !duplicate,
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
