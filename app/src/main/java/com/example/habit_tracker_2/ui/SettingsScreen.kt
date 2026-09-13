package com.example.habit_tracker_2.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habit_tracker_2.data.AppPreferences
import com.example.habit_tracker_2.data.HabitUi
import com.example.habit_tracker_2.data.LabelUi
import com.example.habit_tracker_2.data.ThemeMode
import com.example.habit_tracker_2.data.weekStart
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Full-screen settings, opened from the account button in [HabitTopBar]: account, labels,
 * preferences and about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HabitViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val habits by viewModel.habits.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

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
            HorizontalDivider()
            SettingsSection("Preferences") {
                PreferencesSection(
                    preferences = preferences,
                    onThemeChange = viewModel::setTheme,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onFirstDayOfWeekChange = viewModel::setFirstDayOfWeek,
                    onTrendsRangeChange = viewModel::setTrendsRange,
                )
            }
            HorizontalDivider()
            SettingsSection("About") { AboutSection() }
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

private enum class PreferenceDialog { Theme, WeekStart, TrendsRange }

/** Theme, colors, week start and Trends' opening range. Each takes effect immediately. */
@Composable
private fun PreferencesSection(
    preferences: AppPreferences,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onFirstDayOfWeekChange: (DayOfWeek?) -> Unit,
    onTrendsRangeChange: (TrendRange) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<PreferenceDialog?>(null) }
    val locale = Locale.getDefault()
    fun dayName(day: DayOfWeek) = day.getDisplayName(TextStyle.FULL, locale)
    fun weekStartText(day: DayOfWeek?) =
        day?.let(::dayName) ?: "System default (${dayName(AppPreferences().weekStart(locale))})"
    val trendsRange = trendRangeNamed(preferences.trendsRange)

    ChoiceRow("Theme", themeText(preferences.theme)) { dialog = PreferenceDialog.Theme }
    ListItem(
        headlineContent = { Text("Dynamic color") },
        supportingContent = { Text("Match app colors to your wallpaper") },
        trailingContent = { Switch(checked = preferences.dynamicColor, onCheckedChange = null) },
        modifier = Modifier.toggleable(
            value = preferences.dynamicColor,
            role = Role.Switch,
            onValueChange = onDynamicColorChange,
        ),
    )
    ChoiceRow("Week starts on", weekStartText(preferences.firstDayOfWeek)) { dialog = PreferenceDialog.WeekStart }
    ChoiceRow("Trends opens on", trendsRangeText(trendsRange)) { dialog = PreferenceDialog.TrendsRange }

    val close = { dialog = null }
    when (dialog) {
        PreferenceDialog.Theme -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it to themeText(it) },
            selected = preferences.theme,
            onSelect = { onThemeChange(it); close() },
            onDismiss = close,
        )
        PreferenceDialog.WeekStart -> ChoiceDialog(
            title = "Week starts on",
            options = listOf(null, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
                .map { it to weekStartText(it) },
            selected = preferences.firstDayOfWeek,
            onSelect = { onFirstDayOfWeekChange(it); close() },
            onDismiss = close,
        )
        PreferenceDialog.TrendsRange -> ChoiceDialog(
            title = "Trends opens on",
            options = TrendRange.entries.map { it to trendsRangeText(it) },
            selected = trendsRange,
            onSelect = { onTrendsRangeChange(it); close() },
            onDismiss = close,
        )
        null -> Unit
    }
}

/** A setting that opens a dialog of choices, showing the current one underneath. */
@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Single-choice dialog; picking an option applies it and closes the dialog. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = value == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(value) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            .versionName
    }
    ListItem(
        headlineContent = { Text("Version") },
        supportingContent = { Text(version ?: "Unknown") },
    )
}

private fun themeText(theme: ThemeMode): String = when (theme) {
    ThemeMode.System -> "System default"
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
}

private fun trendsRangeText(range: TrendRange): String = when (range) {
    TrendRange.Week -> "7 days"
    TrendRange.Month -> "30 days"
    TrendRange.Quarter -> "3 months"
    TrendRange.Year -> "1 year"
}
