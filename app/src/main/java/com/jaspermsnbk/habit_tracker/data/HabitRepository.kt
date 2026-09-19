package com.jaspermsnbk.habit_tracker.data

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
 * @param frozenDates every day protected by a streak freeze instead of an actual completion
 * @param freezesAvailable how many streak freezes this habit currently has banked
 * @param last7Frozen parallel to [last7]: which of the last 7 days were frozen rather than completed
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
    val frozenDates: Set<LocalDate>,
    val freezesAvailable: Int,
    val last7Frozen: List<Boolean>,
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
        combine(
            dao.observeHabits(),
            dao.observeAllEntries(),
            dao.observeLabels(),
            dao.observeFreezes(),
        ) { habits, entries, labels, freezes ->
            val today = LocalDate.now()
            val datesByHabit: Map<String, Set<LocalDate>> =
                entries.groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.date }.toSet() }
            val frozenByHabit: Map<String, Set<LocalDate>> =
                freezes.groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.date }.toSet() }
            val labelNames = labels.associate { it.id to it.name }

            habits.map { habit ->
                val dates = datesByHabit[habit.id].orEmpty()
                val frozen = frozenByHabit[habit.id].orEmpty()
                HabitUi(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    doneToday = today in dates,
                    currentStreak = currentStreak(dates + frozen, today),
                    last7 = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in dates },
                    completedDates = dates,
                    frozenDates = frozen,
                    freezesAvailable = habit.freezesAvailable,
                    last7Frozen = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in frozen },
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

    /**
     * Renames a label unless the new name is blank or taken by another label ignoring case,
     * so changing only a label's capitalization is allowed.
     * @return whether the label was renamed
     */
    suspend fun renameLabel(labelId: String, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val sameName = dao.findLabelByName(trimmed)
        if (sameName != null && sameName.id != labelId) return false
        dao.renameLabel(labelId, trimmed)
        return true
    }

    /** Deletes a label. Its habits stay, just without a label. */
    suspend fun deleteLabel(labelId: String) {
        dao.deleteLabel(labelId)
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
        syncFreezeState(habitId)
    }

    /**
     * Spends one of this habit's banked streak freezes to retroactively protect [date], a
     * missed past day, so it counts toward the streak as if it had been completed. A no-op if
     * there's no freeze to spend, the day already has a real entry, or it's already frozen.
     */
    suspend fun useFreeze(habitId: String, date: LocalDate) {
        val habit = dao.getHabit(habitId) ?: return
        if (habit.freezesAvailable <= 0) return
        if (dao.findEntry(habitId, date) != null) return
        if (dao.findFreeze(habitId, date) != null) return
        dao.insertFreeze(
            HabitFreezeEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = date,
                createdAt = Instant.now(),
            )
        )
        dao.updateFreezeState(habitId, habit.freezesAvailable - 1, habit.freezeMilestone)
        syncFreezeState(habitId) // bridging a gap can itself cross a fresh 7-day mark
    }

    /**
     * Changes a habit's name, color and label. Its check-ins, streaks and start date stay as they are.
     * @return whether the habit was updated; a blank name is rejected
     */
    suspend fun updateHabit(habitId: String, name: String, color: String, labelId: String?): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        dao.updateHabit(habitId, trimmed, color, labelId, Instant.now())
        return true
    }

    suspend fun deleteHabit(habitId: String) {
        dao.archiveHabit(habitId, Instant.now())
    }

    /** Permanently wipes every habit, completion, freeze and label. This can't be undone. */
    suspend fun deleteAllData() {
        dao.deleteAllEntries()
        dao.deleteAllFreezes()
        dao.deleteAllHabits()
        dao.deleteAllLabels()
    }

    /**
     * Awards or clamps this habit's banked streak freezes based on its current streak.
     *
     * The milestone tracking is deliberately based on the streak *through yesterday*, a value
     * that can't change no matter how many times today gets toggled within the same day (it
     * excludes today entirely). We award based on *today's* streak, but only ever clamp the
     * milestone down using *yesterday's* streak — and clamp to yesterday's streak + 1, not plain
     * yesterday's streak, because today can always still reach yesterday's streak + 1 by toggling
     * on. Clamping to plain yesterday's streak would wrongly erase a same-day award the instant
     * today gets toggled off.
     *
     * Without this care, the naive approach (recompute streak, award on a fresh multiple of 7,
     * clamp the stored milestone down whenever streak < milestone) lets a user farm unlimited
     * freezes: toggle today off (streak 7 -> 6, milestone clamped 7 -> 6), toggle back on (streak
     * is 7 again, > milestone(6), re-awards). Repeat forever.
     */
    private suspend fun syncFreezeState(habitId: String) {
        val habit = dao.getHabit(habitId) ?: return
        val today = LocalDate.now()
        val protectedDates = (dao.entryDatesFor(habitId) + dao.freezeDatesFor(habitId)).toSet()
        val streakThroughYesterday = strictStreak(protectedDates - today, today.minusDays(1))
        val streakThroughToday = currentStreak(protectedDates, today)

        var milestone = minOf(habit.freezeMilestone, streakThroughYesterday + 1)
        var freezes = habit.freezesAvailable
        if (streakThroughToday > milestone && streakThroughToday % FREEZE_MILESTONE_INTERVAL == 0) {
            freezes = minOf(freezes + 1, MAX_HABIT_FREEZES)
            milestone = streakThroughToday
        }
        if (milestone != habit.freezeMilestone || freezes != habit.freezesAvailable) {
            dao.updateFreezeState(habitId, freezes, milestone)
        }
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

/**
 * Like [currentStreak], but without the leniency that forgives [end] itself being missing.
 * Needed to measure a streak through a day that's unambiguously over (yesterday or earlier),
 * where that leniency would wrongly treat the day as still open.
 */
private fun strictStreak(dates: Set<LocalDate>, end: LocalDate): Int {
    var cursor = end
    var streak = 0
    while (cursor in dates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

internal const val MAX_HABIT_FREEZES = 3
internal const val FREEZE_MILESTONE_INTERVAL = 7
