package com.jaspermsnbk.habit_tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.jaspermsnbk.habit_tracker.data.HabitUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val habits by viewModel.habits.collectAsState()
    val labels by viewModel.labels.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddLabelDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HabitUi?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }
    // Falls back to "All labels" if the selected label no longer exists.
    val selectedLabel = labels.find { it.id == selectedLabelId }
    val visibleHabits = if (selectedLabel == null) habits else habits.filter { it.labelId == selectedLabel.id }

    Scaffold(
        modifier = modifier,
        topBar = { HabitTopBar("Habits", onOpenSettings) },
        floatingActionButton = {
            AddSpeedDial(
                onAddHabit = { showAddDialog = true },
                onAddLabel = { showAddLabelDialog = true },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (labels.isNotEmpty()) {
                LabelFilter(
                    labels = labels,
                    selected = selectedLabel,
                    onSelect = { selectedLabelId = it },
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )
            }
            when {
                habits.isEmpty() -> EmptyState("No habits yet.\nTap + to add your first one.")
                visibleHabits.isEmpty() -> EmptyState("No habits with this label yet.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleHabits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onToggle = { viewModel.toggleToday(habit.id) },
                            onEdit = { editingId = habit.id },
                            onDelete = { pendingDelete = habit },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        HabitDialog(
            title = "New habit",
            confirmText = "Add",
            labels = labels,
            initialLabelId = selectedLabel?.id,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, labelId, emoji ->
                viewModel.addHabit(name, color, labelId, emoji)
                showAddDialog = false
            },
        )
    }

    // Looked up by id, so the dialog closes by itself if the habit is deleted meanwhile.
    habits.find { it.id == editingId }?.let { habit ->
        HabitDialog(
            title = "Edit habit",
            confirmText = "Save",
            labels = labels,
            initialName = habit.name,
            initialColor = habit.color,
            initialEmoji = habit.emoji,
            initialLabelId = habit.labelId,
            onDismiss = { editingId = null },
            onConfirm = { name, color, labelId, emoji ->
                viewModel.updateHabit(habit.id, name, color, labelId, emoji)
                editingId = null
            },
        )
    }

    if (showAddLabelDialog) {
        LabelNameDialog(
            title = "New label",
            confirmText = "Add",
            description = NEW_LABEL_DESCRIPTION,
            existingNames = labels.map { it.name },
            onDismiss = { showAddLabelDialog = false },
            onConfirm = { name ->
                viewModel.addLabel(name)
                showAddLabelDialog = false
            },
        )
    }

    pendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete habit?") },
            text = { Text("\"${habit.name}\" will be removed from your list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habit.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

/** The + button. Tapping it reveals the "Add habit" and "Add label" actions above it. */
@Composable
private fun AddSpeedDial(onAddHabit: () -> Unit, onAddLabel: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "speedDialRotation")

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                // Centers the smaller action icons over the main button.
                modifier = Modifier.padding(end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpeedDialItem("Add label", Icons.AutoMirrored.Outlined.Label) {
                    expanded = false
                    onAddLabel()
                }
                SpeedDialItem("Add habit", Icons.Outlined.AddTask) {
                    expanded = false
                    onAddHabit()
                }
            }
        }
        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (expanded) "Close add menu" else "Add",
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

/** One speed dial action: a text label beside a small icon, clickable as a single button. */
@Composable
private fun SpeedDialItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Surface(
            shape = FloatingActionButtonDefaults.smallShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 3.dp,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitCard(
    habit: HabitUi,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = parseColor(habit.color)
    Card(modifier = Modifier.fillMaxWidth()) {
        // Tapping anywhere but the done toggle or delete button edits the habit.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Edit habit", onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DoneToggle(done = habit.doneToday, accent = accent, onClick = onToggle)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    habit.emoji?.let { emoji ->
                        Text(emoji, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    habit.labelName?.let { labelName ->
                        Spacer(Modifier.width(8.dp))
                        LabelPill(labelName)
                    }
                }
                Spacer(Modifier.size(4.dp))
                StreakRow(streak = habit.currentStreak, freezesAvailable = habit.freezesAvailable)
                Spacer(Modifier.size(8.dp))
                WeekRow(last7 = habit.last7, last7Frozen = habit.last7Frozen, accent = accent)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete habit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelPill(name: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Label: $name" },
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DoneToggle(done: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (done) accent else Color.Transparent,
                shape = CircleShape,
            )
            .border(width = 2.dp, color = accent, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = "Done today", tint = Color.White)
        }
    }
}

@Composable
private fun StreakRow(streak: Int, freezesAvailable: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (streak == 0) "No streak yet" else "$streak day streak",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (freezesAvailable > 0) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.AcUnit,
                contentDescription = null,
                tint = FREEZE_COLOR,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                "$freezesAvailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Seven dots for the last week; filled = completed, icy = protected by a freeze, last dot = today. */
@Composable
private fun WeekRow(last7: List<Boolean>, last7Frozen: List<Boolean>, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        last7.forEachIndexed { index, done ->
            val frozen = last7Frozen.getOrElse(index) { false }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = when {
                            done -> accent
                            frozen -> FREEZE_COLOR
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    )
            )
        }
    }
}

/** A fixed icy blue for frozen-day indicators, readable against both light and dark surfaces. */
internal val FREEZE_COLOR = Color(0xFF4FC3F7)

/** Parse a "#RRGGBB" string; fall back to a neutral color if malformed. */
internal fun parseColor(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }
        .getOrDefault(Color(0xFF6650A4))
