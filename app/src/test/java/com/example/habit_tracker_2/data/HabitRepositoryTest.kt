package com.example.habit_tracker_2.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addHabit_appearsTrimmedAndNotDone() = runBlocking {
        repository.addHabit("  Read  ", "#2E7D32")

        val habit = repository.habits.first().single()
        assertEquals("Read", habit.name)
        assertEquals("#2E7D32", habit.color)
        assertFalse(habit.doneToday)
        assertEquals(0, habit.currentStreak)
        assertEquals(List(7) { false }, habit.last7)
    }

    @Test
    fun toggleToday_marksDoneThenUndone() = runBlocking {
        repository.addHabit("Read", "#2E7D32")
        val id = repository.habits.first().single().id

        repository.toggleToday(id)
        repository.habits.first().single().let {
            assertTrue(it.doneToday)
            assertEquals(1, it.currentStreak)
            assertTrue(it.last7.last())
        }

        repository.toggleToday(id)
        repository.habits.first().single().let {
            assertFalse(it.doneToday)
            assertEquals(0, it.currentStreak)
            assertFalse(it.last7.last())
        }
    }

    @Test
    fun pastEntries_feedStreakAndWeek() = runBlocking {
        repository.addHabit("Read", "#2E7D32")
        val id = repository.habits.first().single().id
        val today = LocalDate.now()
        listOf(1L, 2L).forEach { offset ->
            db.habitDao().insertEntry(
                HabitEntryEntity(UUID.randomUUID().toString(), id, today.minusDays(offset), Instant.now())
            )
        }

        val habit = repository.habits.first().single()
        assertFalse(habit.doneToday)
        assertEquals(2, habit.currentStreak)
        assertEquals(listOf(false, false, false, false, true, true, false), habit.last7)
    }

    @Test
    fun deleteHabit_removesItFromList() = runBlocking {
        repository.addHabit("Read", "#2E7D32")
        repository.addHabit("Run", "#1565C0")
        val read = repository.habits.first().first { it.name == "Read" }

        repository.deleteHabit(read.id)

        assertEquals(listOf("Run"), repository.habits.first().map { it.name })
    }
}
