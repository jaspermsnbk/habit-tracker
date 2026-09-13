package com.example.habit_tracker_2.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A habit plus its derived, display-ready state. The UI reads this and nothing else.
 *
 * @param last7 completion flags for the 7 days ending today (index 0 = 6 days ago, index 6 = today)
 */
data class HabitUi(
    val id: String,
    val name: String,
    val color: String,
    val doneToday: Boolean,
    val currentStreak: Int,
    val last7: List<Boolean>,
)

/**
 * Single point of access to habit data. In Phase 1 it's Room-only; in Phase 3 this is
 * where the backend sync is added, without the UI or ViewModel needing to change.
 */
class HabitRepository(private val dao: HabitDao) {

    /** Live stream of habits with streaks computed. Recombines whenever data changes. */
    val habits: Flow<List<HabitUi>> =
        combine(dao.observeHabits(), dao.observeAllEntries()) { habits, entries ->
            val today = LocalDate.now()
            val datesByHabit: Map<String, Set<LocalDate>> =
                entries.groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.date }.toSet() }

            habits.map { habit ->
                val dates = datesByHabit[habit.id].orEmpty()
                HabitUi(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    doneToday = today in dates,
                    currentStreak = currentStreak(dates, today),
                    last7 = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) in dates },
                )
            }
        }

    suspend fun addHabit(name: String, color: String) {
        val now = Instant.now()
        dao.upsertHabit(
            HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = color,
                createdAt = now,
                updatedAt = now,
            )
        )
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
