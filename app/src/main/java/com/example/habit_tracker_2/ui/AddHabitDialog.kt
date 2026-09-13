package com.example.habit_tracker_2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.habit_tracker_2.data.LabelUi

/** Preset colors the user can tag a habit with. */
private val PRESET_COLORS = listOf(
    "#6650A4", // purple
    "#2E7D32", // green
    "#1565C0", // blue
    "#EF6C00", // orange
    "#C2185B", // pink
    "#00838F", // teal
)

/**
 * @param labels labels the habit can be filed under
 * @param initialLabelId label to preselect, e.g. the one the list is currently filtered by
 */
@Composable
fun AddHabitDialog(
    labels: List<LabelUi>,
    initialLabelId: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: String, labelId: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PRESET_COLORS.first()) }
    var selectedLabelId by remember { mutableStateOf(initialLabelId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(16.dp))
                Text("Color")
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PRESET_COLORS.forEach { hex ->
                        ColorSwatch(
                            hex = hex,
                            selected = hex == selectedColor,
                            onClick = { selectedColor = hex },
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                Text("Label")
                Spacer(Modifier.size(8.dp))
                if (labels.isEmpty()) {
                    Text(
                        "No labels yet. Add one from the + menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedLabelId == null,
                            onClick = { selectedLabelId = null },
                            label = { Text("None") },
                        )
                        labels.forEach { label ->
                            FilterChip(
                                selected = selectedLabelId == label.id,
                                onClick = { selectedLabelId = label.id },
                                label = { Text(label.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedColor, selectedLabelId) },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = Color(hex.toColorInt())
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = color, shape = CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
        }
    }
}
