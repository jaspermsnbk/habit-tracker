package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.jaspermsnbk.habit_tracker.data.LabelUi

private class PresetColor(val hex: String, val name: String)

/** Preset colors the user can tag a habit with. */
private val PRESET_COLORS = listOf(
    PresetColor("#6650A4", "Purple"),
    PresetColor("#2E7D32", "Green"),
    PresetColor("#1565C0", "Blue"),
    PresetColor("#EF6C00", "Orange"),
    PresetColor("#C2185B", "Pink"),
    PresetColor("#00838F", "Teal"),
)

/**
 * Dialog for a habit's name, color and label, used both to add a habit and to edit one.
 *
 * @param labels labels the habit can be filed under
 * @param initialColor preselected color; if it isn't a preset, it's kept unless another is picked
 * @param initialLabelId label to preselect: the habit's own, or the one the list is filtered by
 */
@Composable
fun HabitDialog(
    title: String,
    confirmText: String,
    labels: List<LabelUi>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: String, labelId: String?) -> Unit,
    initialName: String = "",
    initialColor: String = PRESET_COLORS.first().hex,
    initialLabelId: String? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var selectedColor by rememberSaveable { mutableStateOf(initialColor) }
    var selectedLabelId by rememberSaveable { mutableStateOf(initialLabelId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PRESET_COLORS.forEach { preset ->
                        ColorSwatch(
                            preset = preset,
                            selected = preset.hex.equals(selectedColor, ignoreCase = true),
                            onClick = { selectedColor = preset.hex },
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
                            // A label deleted while the dialog is open counts as none.
                            selected = labels.none { it.id == selectedLabelId },
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
                onClick = {
                    onConfirm(name, selectedColor, selectedLabelId?.takeIf { id -> labels.any { it.id == id } })
                },
                enabled = name.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColorSwatch(preset: PresetColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = Color(preset.hex.toColorInt()), shape = CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = preset.name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}
