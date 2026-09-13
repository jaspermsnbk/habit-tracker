package com.example.habit_tracker_2.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * A habit plus its derived, display-ready state. The UI reads this and nothing else.
 *
 * @param last7 completion flags for the 7 days ending today (index 0 = 6 days ago, index 6 = today)
 * @param completedDates every day this habit was completed, for the calendar
 * @param labelName the name of the habit's label, or null if it has none
 * @param createdOn the local day the habit was added, so trends don't count earlier days as missed
 */
data class HabitUi(
    val id: String,
    val name: String,
    val color: String,
    val doneToday: Boolean,
    val currentStreak: Int,
    val last7: List<Boolean>,
    val completedDates: Set<LocalDate>,
    val labelId: String?,
    val labelName: String?,
    val createdOn: LocalDate,
)

/** A label as the UI sees it. */
data class LabelUi(
    val id: String,
    val name: String,
)

/**
 * Single point of access to habit data. In Phase 1 it's Room-only; in Phase 3 this is
 * where the backend sync is added, without the UI or ViewModel needing to change.
 */
class HabitRepository(private val dao: HabitDao) {

    /** Live stream of labels, sorted by name ignoring case. */
    val labels: Flow<List<LabelUi>> =
        dao.observeLabels().map { labels -> labels.map { LabelUi(id = it.id, name = it.name) } }

    /** Live stream of habits with streaks computed. Recombines whenever data changes. */
    val habits: Flow<List<HabitUi>> =
        combine(dao.observeHabits(), dao.observeAllEntries(), dao.observeLabels()) { habits, entries, labels ->
            val today = LocalDate.now()
            val datesByHabit: Map<String, Set<LocalDate>> =
                entries.groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.date }.toSet() }
            val labelNames = labels.associate { it.id to it.name }

            habits.map { habit ->
                val dates = datesByHabit[habit.id].orEmpty()
                HabitUi(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    doneToday = today in dates,
                    currentStreak = currentStreak(dates, today),
                    last7 = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in dates },
                    completedDates = dates,
                    labelId = habit.labelId,
                    labelName = habit.labelId?.let(labelNames::get),
                    createdOn = habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                )
            }
        }

    suspend fun addHabit(name: String, color: String, labelId: String? = null) {
        val now = Instant.now()
        dao.upsertHabit(
            HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = color,
                createdAt = now,
                updatedAt = now,
                labelId = labelId,
            )
        )
    }

    /**
     * Adds a label unless the name is blank or matches an existing label ignoring case.
     * @return whether the label was added
     */
    suspend fun addLabel(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || dao.findLabelByName(trimmed) != null) return false
        dao.insertLabel(
            LabelEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = Instant.now(),
            )
        )
        return true
    }

    /** Toggle today's completion: unmark if already done, otherwise mark done. */
    suspend fun toggleToday(habitId: String) {
        val today = LocalDate.now()
        if (dao.findEntry(habitId, today) != null) {
            dao.deleteEntry(habitId, today)
        } else {
            dao.insertEntry(
                HabitEntryEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = today,
                    createdAt = Instant.now(),
                )
            )
        }
    }

    suspend fun deleteHabit(habitId: String) {
        dao.archiveHabit(habitId, Instant.now())
    }
}

/**
 * Count consecutive completed days ending today (or yesterday). Missing today does not
 * break a streak until the day is over — so a streak through yesterday still counts.
 */
internal fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
    var cursor = if (today in dates) today else today.minusDays(1)
    var streak = 0
    while (cursor in dates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
