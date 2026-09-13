package com.example.habit_tracker_2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habit_tracker_2.data.HabitUi
import com.example.habit_tracker_2.data.LabelUi

/**
 * Full-screen settings, opened from the account button in [HabitTopBar]. Sections (account,
 * labels, preferences, about) are added here one at a time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HabitViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val habits by viewModel.habits.collectAsState()
    val labels by viewModel.labels.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Account") { AccountSection() }
            HorizontalDivider()
            SettingsSection("Labels") {
                LabelsSection(
                    labels = labels,
                    habits = habits,
                    onAdd = { viewModel.addLabel(it) },
                    onRename = { id, name -> viewModel.renameLabel(id, name) },
                    onDelete = { viewModel.deleteLabel(it) },
                )
            }
        }
    }
}

/** A titled group of settings rows. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        content()
    }
}

/**
 * Everyone inside the app is a guest for now (see [com.example.habit_tracker_2.data.SessionStore]);
 * accounts arrive with the backend, which is when "Sign in" gets enabled.
 */
@Composable
private fun AccountSection() {
    ListItem(
        headlineContent = { Text("Guest") },
        supportingContent = { Text("Your habits are saved on this phone only. Signing in is coming soon.") },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        trailingContent = {
            OutlinedButton(onClick = {}, enabled = false) { Text("Sign in") }
        },
    )
}

/** Every label with its habit count. Tap a label to rename it; the trash icon deletes it. */
@Composable
private fun LabelsSection(
    labels: List<LabelUi>,
    habits: List<HabitUi>,
    onAdd: (name: String) -> Unit,
    onRename: (labelId: String, name: String) -> Unit,
    onDelete: (labelId: String) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val habitCounts = habits.groupingBy { it.labelId }.eachCount()

    if (labels.isEmpty()) {
        Text(
            "No labels yet. $NEW_LABEL_DESCRIPTION",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    labels.forEach { label ->
        ListItem(
            headlineContent = { Text(label.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(habitCountText(habitCounts[label.id] ?: 0)) },
            leadingContent = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = { deletingId = label.id }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${label.name}")
                }
            },
            modifier = Modifier.clickable(onClickLabel = "Rename") { renamingId = label.id },
        )
    }
    ListItem(
        headlineContent = { Text("New label", color = MaterialTheme.colorScheme.primary) },
        leadingContent = {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable { adding = true },
    )

    if (adding) {
        LabelNameDialog(
            title = "New label",
            confirmText = "Add",
            description = NEW_LABEL_DESCRIPTION,
            existingNames = labels.map { it.name },
            onDismiss = { adding = false },
            onConfirm = { name ->
                onAdd(name)
                adding = false
            },
        )
    }

    // Dialogs look labels up by id, so they close by themselves if the label disappears.
    labels.find { it.id == renamingId }?.let { label ->
        LabelNameDialog(
            title = "Rename label",
            confirmText = "Rename",
            initialName = label.name,
            existingNames = labels.filter { it.id != label.id }.map { it.name },
            onDismiss = { renamingId = null },
            onConfirm = { name ->
                onRename(label.id, name)
                renamingId = null
            },
        )
    }

    labels.find { it.id == deletingId }?.let { label ->
        val count = habitCounts[label.id] ?: 0
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Delete label?") },
            text = {
                Text(
                    "\"${label.name}\" will be removed." + when (count) {
                        0 -> ""
                        1 -> " Its habit stays, just without a label."
                        else -> " Its $count habits stay, just without a label."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(label.id)
                        deletingId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("Cancel") }
            },
        )
    }
}

private fun habitCountText(count: Int): String = when (count) {
    0 -> "No habits"
    1 -> "1 habit"
    else -> "$count habits"
}
