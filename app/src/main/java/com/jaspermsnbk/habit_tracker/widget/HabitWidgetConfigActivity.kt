package com.jaspermsnbk.habit_tracker.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.jaspermsnbk.habit_tracker.HabitApplication
import com.jaspermsnbk.habit_tracker.data.LabelUi
import com.jaspermsnbk.habit_tracker.data.ThemeMode
import com.jaspermsnbk.habit_tracker.ui.LabelFilter
import com.jaspermsnbk.habit_tracker.ui.theme.HabitTrackerTheme
import kotlinx.coroutines.launch

/**
 * Asks which label a widget should show, when it is placed and again whenever the user
 * reconfigures it. The choice is kept per widget instance (see [widgetLabelIdKey]), so two
 * widgets can sit side by side showing different labels.
 */
class HabitWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Backing out has to leave the widget unplaced, so cancelled is the starting answer.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as HabitApplication

        setContent {
            val preferences by app.preferencesStore.preferences.collectAsState()
            val darkTheme = when (preferences.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val labels by app.repository.labels.collectAsState(initial = emptyList())
            var selectedLabelId by remember { mutableStateOf<String?>(null) }

            // Reconfiguring an existing widget opens on the label it is already showing.
            LaunchedEffect(Unit) {
                selectedLabelId = savedLabelId()
            }

            HabitTrackerTheme(darkTheme = darkTheme, dynamicColor = preferences.dynamicColor) {
                ConfigScreen(
                    labels = labels,
                    selected = labels.find { it.id == selectedLabelId },
                    onSelect = { selectedLabelId = it },
                    onCancel = { finish() },
                    onConfirm = { confirm(selectedLabelId) },
                )
            }
        }
    }

    /** The label this widget was last configured with, or null for "all labels". */
    private suspend fun savedLabelId(): String? {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        return getAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId)[widgetLabelIdKey]
    }

    private fun confirm(labelId: String?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@HabitWidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@HabitWidgetConfigActivity, glanceId) { preferences ->
                if (labelId == null) {
                    preferences.remove(widgetLabelIdKey)
                } else {
                    preferences[widgetLabelIdKey] = labelId
                }
            }
            HabitWidget.update(this@HabitWidgetConfigActivity, glanceId)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    /** The launcher identifies which widget it asked about by the id coming back. */
    private fun resultIntent() =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(
    labels: List<LabelUi>,
    selected: LabelUi?,
    onSelect: (String?) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Widget habits") }) },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (labels.isEmpty()) {
                    "This widget will show all of your habits. Add labels in the app to show only some of them."
                } else {
                    "Choose which habits this widget shows."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (labels.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LabelFilter(labels = labels, selected = selected, onSelect = onSelect)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConfirm) { Text("Done") }
            }
        }
    }
}
