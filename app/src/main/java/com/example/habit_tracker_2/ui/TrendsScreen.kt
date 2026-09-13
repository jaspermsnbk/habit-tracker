package com.example.habit_tracker_2.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

internal const val TREND_CHART_TAG = "trendChart"
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * Completion history over a chosen window: headline stats, a bar chart of check-ins per day or
 * week (tap or drag to inspect a bar), a weekday pattern, and a per-habit breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(viewModel: HabitViewModel, modifier: Modifier = Modifier) {
    val habits by viewModel.habits.collectAsState()
    val labels by viewModel.labels.collectAsState()
    var range by rememberSaveable { mutableStateOf(TrendRange.Month) }
    var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    // Selections fall back to "All" if the label or habit is deleted. Everything below the
    // label filter only sees that label's habits.
    val selectedLabel = labels.find { it.id == selectedLabelId }
    val labelHabits = if (selectedLabel == null) habits else habits.filter { it.labelId == selectedLabel.id }
    val selected = labelHabits.find { it.id == selectedHabitId }
    val today = LocalDate.now()
    val trends = remember(labelHabits, selected, range, today) {
        computeTrends(if (selected != null) listOf(selected) else labelHabits, range, today)
    }
    // The chart bar being inspected; bars mean something else once the range or selection changes.
    var inspected by rememberSaveable(range, selectedLabel?.id, selected?.id) { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Trends") }) },
    ) { innerPadding ->
        if (habits.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Add a habit to see your trends here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val accent = selected?.let { parseColor(it.color) } ?: MaterialTheme.colorScheme.primary
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
                if (labelHabits.isEmpty()) {
                    Spacer(Modifier.size(24.dp))
                    Text(
                        "No habits with this label yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    return@Column
                }
                HabitFilter(labelHabits, selected, onSelect = { selectedHabitId = it })
                Spacer(Modifier.size(12.dp))
                RangeSelector(range, onSelect = { range = it })
                Spacer(Modifier.size(16.dp))
                StatTiles(trends)

                SectionTitle(if (range.bucketDays == 1) "Check-ins per day" else "Check-ins per week")
                InspectedBar(trends, range, inspected, today)
                Spacer(Modifier.size(8.dp))
                TrendChart(
                    trends = trends,
                    range = range,
                    barColor = accent,
                    inspected = inspected,
                    onTap = { i -> inspected = if (inspected == i) null else i },
                    onScrub = { i -> inspected = i },
                )

                // A week has one of each weekday, which the chart above already shows.
                if (range != TrendRange.Week && trends.weekdayRates.isNotEmpty()) {
                    SectionTitle("By weekday")
                    WeekdayPattern(trends.weekdayRates, accent)
                }

                if (selected == null && labelHabits.size > 1) {
                    SectionTitle("By habit")
                    HabitBreakdown(trends.perHabit, onSelect = { selectedHabitId = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(range: TrendRange, onSelect: (TrendRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TrendRange.entries.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == range,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = TrendRange.entries.size),
            ) {
                Text(option.label)
            }
        }
    }
}

@Composable
private fun StatTiles(trends: Trends) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(trends.completed.toString(), "Check-ins", Modifier.weight(1f))

        val delta = trends.previousRate?.let { ((trends.rate - it) * 100).roundToInt() }
        StatTile(
            value = percent(trends.rate),
            label = "Completion",
            modifier = Modifier.weight(1f),
            footnote = delta?.let {
                when {
                    it > 0 -> "↑ $it pts vs. before"
                    it < 0 -> "↓ ${-it} pts vs. before"
                    else -> "Same as before"
                }
            },
            footnoteColor = when {
                delta == null || delta == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                delta > 0 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            },
        )

        StatTile(days(trends.bestStreak), "Best streak", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier,
    footnote: String? = null,
    footnoteColor: Color = Color.Unspecified,
) {
    Card(
        modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {}
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (footnote != null) {
                Spacer(Modifier.size(4.dp))
                Text(footnote, style = MaterialTheme.typography.labelSmall, color = footnoteColor)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.size(24.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.size(4.dp))
}

/** Details of the inspected bar, or the window's average with a hint when nothing is inspected. */
@Composable
private fun InspectedBar(trends: Trends, range: TrendRange, inspected: Int?, today: LocalDate) {
    val bucket = inspected?.let(trends.buckets::getOrNull)
    val text = if (bucket != null) {
        "${bucketLabel(bucket, today)}: ${bucket.completed} of ${bucket.possible}" +
            if (bucket.possible > 0) " (${percent(bucket.rate)})" else ""
    } else {
        val average = trends.completed.toFloat() / trends.buckets.size
        val unit = if (range.bucketDays == 1) "day" else "week"
        "Avg. ${String.format(Locale.getDefault(), "%.1f", average)} per $unit · tap a bar for details"
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (bucket != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Bars of check-ins per bucket over a faint bar of what was possible, with a dashed line at
 * the average. The y-axis tops out at the most habit-days any bucket could have had.
 */
@Composable
private fun TrendChart(
    trends: Trends,
    range: TrendRange,
    barColor: Color,
    inspected: Int?,
    onTap: (Int) -> Unit,
    onScrub: (Int) -> Unit,
) {
    val buckets = trends.buckets
    val maxY = maxOf(1, buckets.maxOf { it.possible })
    val average = trends.completed.toFloat() / buckets.size
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val averageColor = MaterialTheme.colorScheme.outline
    // Gesture detectors outlive recompositions, so they read the latest callbacks through these.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val axisWidth = 32.dp
    val description = "Bar chart of check-ins over the last ${range.days} days: " +
        "${trends.completed} of ${trends.possible}"

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Column(
                Modifier
                    .width(axisWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(maxY.toString())
                AxisLabel("0")
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(TREND_CHART_TAG)
                    .semantics { contentDescription = description }
                    .pointerInput(buckets.size) {
                        detectTapGestures { offset ->
                            currentOnTap(barIndexAt(offset.x, size.width.toFloat(), buckets.size))
                        }
                    }
                    .pointerInput(buckets.size) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            currentOnScrub(barIndexAt(change.position.x, size.width.toFloat(), buckets.size))
                        }
                    }
            ) {
                val slot = size.width / buckets.size
                val gap = minOf(slot * 0.3f, 6.dp.toPx())
                val barWidth = slot - gap
                val cornerRadius = CornerRadius(minOf(barWidth / 2, 4.dp.toPx()))
                fun heightOf(value: Int) = size.height * value / maxY

                buckets.forEachIndexed { i, bucket ->
                    val left = i * slot + gap / 2
                    if (bucket.possible > 0) {
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, size.height - heightOf(bucket.possible)),
                            size = Size(barWidth, heightOf(bucket.possible)),
                            cornerRadius = cornerRadius,
                        )
                    }
                    if (bucket.completed > 0) {
                        drawRoundRect(
                            color = if (inspected == null || inspected == i) barColor else barColor.copy(alpha = 0.35f),
                            topLeft = Offset(left, size.height - heightOf(bucket.completed)),
                            size = Size(barWidth, heightOf(bucket.completed)),
                            cornerRadius = cornerRadius,
                        )
                    }
                }

                if (average > 0f) {
                    val y = size.height - size.height * average / maxY
                    drawLine(
                        color = averageColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = axisWidth, top = 4.dp)
        ) {
            AxisLabel(buckets.first().start.format(SHORT_DAY_FORMAT))
            Spacer(Modifier.weight(1f))
            AxisLabel("Today")
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A mini bar per weekday showing how often habits get done on that day. */
@Composable
private fun WeekdayPattern(rates: Map<DayOfWeek, Float>, color: Color) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val best = rates.maxByOrNull { it.value }?.takeIf { it.value > 0f }?.key

    if (best != null) {
        Text(
            "Your most consistent day: ${best.getDisplayName(TextStyle.FULL, locale)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
    }
    Row(Modifier.fillMaxWidth()) {
        repeat(7) { i ->
            val day = firstDayOfWeek.plus(i.toLong())
            val rate = rates[day]
            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = day.getDisplayName(TextStyle.FULL, locale) + ": " +
                            (rate?.let(::percent) ?: "not tracked")
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(20.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(rate ?: 0f)
                            .background(if (day == best) color else color.copy(alpha = 0.55f))
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Each habit's completion rate in the window, best first. Tapping one focuses the screen on it. */
@Composable
private fun HabitBreakdown(perHabit: List<HabitTrend>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        perHabit.forEach { trend ->
            val color = parseColor(trend.habit.color)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "Show trends for ${trend.habit.name}") { onSelect(trend.habit.id) }
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        trend.habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        percent(trend.rate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.size(6.dp))
                LinearProgressIndicator(
                    progress = { trend.rate },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "${trend.completed} of ${days(trend.possible)} · best streak ${days(trend.bestStreak)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** How a chart bar is named in its details: "Today", "Sep 3", "Last 7 days" or "Aug 30 – Sep 5". */
internal fun bucketLabel(bucket: TrendBucket, today: LocalDate): String = when {
    bucket.start == bucket.end ->
        if (bucket.start == today) "Today" else bucket.start.format(SHORT_DAY_FORMAT)
    bucket.end == today -> "Last 7 days"
    else -> "${bucket.start.format(SHORT_DAY_FORMAT)} – ${bucket.end.format(SHORT_DAY_FORMAT)}"
}

/** Which of [count] equal-width bars spans horizontal position [x] of a [width]-wide chart. */
internal fun barIndexAt(x: Float, width: Float, count: Int): Int =
    (x / width * count).toInt().coerceIn(0, count - 1)

private fun percent(rate: Float): String = "${(rate * 100).roundToInt()}%"

private fun days(n: Int): String = "$n ${if (n == 1) "day" else "days"}"
