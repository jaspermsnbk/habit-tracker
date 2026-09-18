package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jaspermsnbk.habit_tracker.data.HabitUi
import com.jaspermsnbk.habit_tracker.data.LabelUi
import com.jaspermsnbk.habit_tracker.data.weekStart
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
internal val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")
internal val SHEET_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
internal const val DAY_HABITS_LIST_TAG = "dayHabitsList"

/**
 * Month view of completed days. "All habits" shows a colored dot per habit done that day;
 * picking one habit fills its completed days with the habit's color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val habits by viewModel.habits.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    // The day whose completed habits are listed in the bottom sheet, if open.
    var openDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    // Selections fall back to "All" if the label or habit is deleted. Everything below the
    // label filter (habit chips, grid, summary, day details) only sees that label's habits.
    val selectedLabel = labels.find { it.id == selectedLabelId }
    val labelHabits = if (selectedLabel == null) habits else habits.filter { it.labelId == selectedLabel.id }
    val selected = labelHabits.find { it.id == selectedHabitId }
    val today = LocalDate.now()

    Scaffold(
        modifier = modifier,
        topBar = { HabitTopBar("Calendar", onOpenSettings) },
    ) { innerPadding ->
        if (habits.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Add a habit to see your history here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (labels.isNotEmpty()) {
                    LabelFilter(
                        labels = labels,
                        selected = selectedLabel,
                        onSelect = {
                            selectedLabelId = it
                            selectedHabitId = null
                        },
                    )
                    Spacer(Modifier.size(8.dp))
                }
                HabitFilter(labelHabits, selected, onSelect = { selectedHabitId = it })
                Spacer(Modifier.size(16.dp))
                MonthHeader(
                    month = month,
                    canGoForward = month < YearMonth.from(today),
                    onPrevious = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                )
                Spacer(Modifier.size(8.dp))
                MonthGrid(
                    month = month,
                    today = today,
                    habits = labelHabits,
                    selected = selected,
                    firstDayOfWeek = preferences.weekStart(),
                    // Day details list every habit, so they're only offered under "All habits".
                    onDayClick = if (selected == null) { day -> openDay = day } else null,
                )
                Spacer(Modifier.size(16.dp))
                MonthSummary(month, labelHabits, selected, selectedLabel)
            }
        }
    }

    openDay?.let { day ->
        DayHabitsSheet(date = day, habits = labelHabits, onDismiss = { openDay = null })
    }
}

@Composable
internal fun HabitFilter(habits: List<HabitUi>, selected: HabitUi?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All habits") },
        )
        habits.forEach { habit ->
            FilterChip(
                selected = selected?.id == habit.id,
                onClick = { onSelect(habit.id) },
                label = { Text(habit.name) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(parseColor(habit.color), CircleShape)
                    )
                },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            month.format(MONTH_FORMAT),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    habits: List<HabitUi>,
    selected: HabitUi?,
    firstDayOfWeek: DayOfWeek,
    onDayClick: ((LocalDate) -> Unit)?,
) {
    val locale = Locale.getDefault()

    Column {
        Row {
            repeat(7) { i ->
                Text(
                    firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        monthCells(month, firstDayOfWeek).chunked(7).forEach { week ->
            Row {
                week.forEach { date ->
                    val cellModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                    if (date == null) {
                        Spacer(cellModifier)
                    } else {
                        DayCell(
                            date = date,
                            today = today,
                            habits = habits,
                            selected = selected,
                            // Nothing can be completed in the future, so those days aren't tappable.
                            onClick = if (onDayClick != null && date <= today) {
                                { onDayClick(date) }
                            } else {
                                null
                            },
                            modifier = cellModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    today: LocalDate,
    habits: List<HabitUi>,
    selected: HabitUi?,
    onClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val done = (if (selected != null) listOf(selected) else habits).filter { date in it.completedDates }
    val fill = if (selected != null && done.isNotEmpty()) parseColor(selected.color) else null
    val description = date.format(DAY_FORMAT) +
        if (done.isEmpty()) ", nothing completed" else ", completed: " + done.joinToString { it.name }

    Box(
        modifier
            .padding(2.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) {
                    onClick(label = "Show completed habits") { onClick(); true }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(fill ?: Color.Transparent)
                .then(
                    if (date == today) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    fill != null -> Color.White
                    date > today -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (selected == null && done.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    done.take(3).forEach { habit ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .background(parseColor(habit.color), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummary(
    month: YearMonth,
    habits: List<HabitUi>,
    selected: HabitUi?,
    selectedLabel: LabelUi?,
) {
    fun countIn(habit: HabitUi) = habit.completedDates.count { YearMonth.from(it) == month }

    val text = if (selected != null) {
        val days = countIn(selected)
        "${selected.name}: $days ${if (days == 1) "day" else "days"} this month"
    } else {
        val checkIns = habits.sumOf(::countIn)
        val prefix = selectedLabel?.let { "${it.name}: " }.orEmpty()
        "$prefix$checkIns ${if (checkIns == 1) "check-in" else "check-ins"} this month"
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Bottom sheet listing the habits completed on [date]. Reads live [habits], so it stays current. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayHabitsSheet(date: LocalDate, habits: List<HabitUi>, onDismiss: () -> Unit) {
    val done = habits.filter { date in it.completedDates }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(date.format(SHEET_DATE_FORMAT), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(4.dp))
            Text(
                if (done.isEmpty()) {
                    "Nothing completed"
                } else {
                    "${done.size} of ${habits.size} ${if (habits.size == 1) "habit" else "habits"} completed"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (done.isNotEmpty()) {
                Spacer(Modifier.size(16.dp))
                LazyColumn(
                    modifier = Modifier.testTag(DAY_HABITS_LIST_TAG),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(done, key = { it.id }) { habit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(parseColor(habit.color), CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The cells of a month grid, row by row: leading nulls pad the first week so day 1 lands
 * under its weekday, and trailing nulls fill out the last week.
 */
internal fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    return cells + List((7 - cells.size % 7) % 7) { null }
}
