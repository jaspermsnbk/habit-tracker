package com.jaspermsnbk.habit_tracker.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TodayFlowTest {

    private val start = LocalDate.of(2026, 10, 12)
    private lateinit var clock: MutableClock
    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        clock = MutableClock(start)
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao(), clock, dayPollMillis = 5)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun todayFlow_emitsTheDate_thenEachNewDate_andNeverRepeats() = runBlocking {
        val seen = async { todayFlow(clock, pollMillis = 5).take(3).toList() }

        delay(40) // several polls on the same day add nothing
        clock.advanceDays(1)
        delay(40)
        clock.advanceDays(1)

        assertEquals(listOf(start, start.plusDays(1), start.plusDays(2)), withTimeout(2_000) { seen.await() })
    }

    @Test
    fun streaks_moveOnAtMidnight_withoutAnyCheckIn() = runBlocking {
        val habitId = "h"
        db.habitDao().upsertHabit(
            HabitEntity(
                id = habitId, name = "Read", color = "#000000",
                createdAt = Instant.parse("2026-10-01T00:00:00Z"), updatedAt = Instant.parse("2026-10-01T00:00:00Z"),
            )
        )
        for (offset in 0L..2L) {
            db.habitDao().insertEntry(HabitEntryEntity("e$offset", habitId, start.minusDays(offset), Instant.now()))
        }

        val streaks = mutableListOf<Int>()
        val job = launch { repository.habits.collect { streaks += it.single().currentStreak } }

        waitUntil { streaks.isNotEmpty() }
        assertEquals(3, streaks.last())
        // The next day, nothing is checked off yet; the streak through yesterday still counts, then
        // the day after that it is broken.
        clock.advanceDays(1)
        waitUntil { streaks.last() == 3 && streaks.size >= 2 }
        clock.advanceDays(1)
        waitUntil { streaks.last() == 0 }
        job.cancel()
        assertTrue(streaks.contains(0))
    }

    @Test
    fun petState_followsTheClock_forAnAdoptedPet() = runBlocking {
        db.habitDao().upsertPet(PetEntity(name = "Momo", bornOn = start))
        db.habitDao().upsertHabit(
            HabitEntity(
                id = "h", name = "Read", color = "#000000",
                createdAt = Instant.parse("2026-10-01T00:00:00Z"), updatedAt = Instant.parse("2026-10-01T00:00:00Z"),
            )
        )

        val moods = mutableListOf<PetMood>()
        val job = launch { repository.pet.collect { it?.let { pet -> moods += pet.state.mood } } }

        waitUntil { moods.isNotEmpty() }
        assertEquals(PetMood.Hungry, moods.last())
        // Four days with nothing done and the pet dies, with no check-in to prompt an update.
        clock.advanceDays(5)
        waitUntil { moods.last() == PetMood.Dead }
        job.cancel()
    }

    private suspend fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting" }
            delay(10)
        }
    }
}
