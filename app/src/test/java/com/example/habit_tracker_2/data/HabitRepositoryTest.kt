package com.example.habit_tracker_2.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(setOf(today.minusDays(1), today.minusDays(2)), habit.completedDates)
    }

    @Test
    fun deleteHabit_removesItFromList() = runBlocking {
        repository.addHabit("Read", "#2E7D32")
        repository.addHabit("Run", "#1565C0")
        val read = repository.habits.first().first { it.name == "Read" }

        repository.deleteHabit(read.id)

        assertEquals(listOf("Run"), repository.habits.first().map { it.name })
    }

    @Test
    fun addLabel_trimsAndSortsByNameIgnoringCase() = runBlocking {
        assertTrue(repository.addLabel(" Learning "))
        assertTrue(repository.addLabel("fitness"))

        assertEquals(listOf("fitness", "Learning"), repository.labels.first().map { it.name })
    }

    @Test
    fun addLabel_rejectsBlankAndDuplicateNamesIgnoringCase() = runBlocking {
        assertTrue(repository.addLabel("Fitness"))

        assertFalse(repository.addLabel("FITNESS"))
        assertFalse(repository.addLabel("   "))
        assertEquals(1, repository.labels.first().size)
    }

    @Test
    fun habitWithLabel_exposesLabelIdAndName() = runBlocking {
        repository.addLabel("Fitness")
        val label = repository.labels.first().single()

        repository.addHabit("Run", "#2E7D32", label.id)
        repository.addHabit("Read", "#1565C0")

        val habits = repository.habits.first().associateBy { it.name }
        assertEquals(label.id, habits.getValue("Run").labelId)
        assertEquals("Fitness", habits.getValue("Run").labelName)
        assertNull(habits.getValue("Read").labelId)
        assertNull(habits.getValue("Read").labelName)
    }

    @Test
    fun renameLabel_trimsAndRenamesForItsHabits() = runBlocking {
        repository.addLabel("Fitness")
        val label = repository.labels.first().single()
        repository.addHabit("Run", "#2E7D32", label.id)

        assertTrue(repository.renameLabel(label.id, "  Exercise "))

        assertEquals("Exercise", repository.labels.first().single().name)
        assertEquals("Exercise", repository.habits.first().single().labelName)
    }

    @Test
    fun renameLabel_rejectsBlankAndOtherLabelsNames_butAllowsChangingCase() = runBlocking {
        repository.addLabel("Fitness")
        repository.addLabel("Learning")
        val fitness = repository.labels.first().first { it.name == "Fitness" }

        assertFalse(repository.renameLabel(fitness.id, " "))
        assertFalse(repository.renameLabel(fitness.id, "LEARNING"))
        assertTrue(repository.renameLabel(fitness.id, "FITNESS"))

        assertEquals(listOf("FITNESS", "Learning"), repository.labels.first().map { it.name })
    }

    @Test
    fun deleteLabel_keepsItsHabitsWithoutALabel() = runBlocking {
        repository.addLabel("Fitness")
        val label = repository.labels.first().single()
        repository.addHabit("Run", "#2E7D32", label.id)

        repository.deleteLabel(label.id)

        assertTrue(repository.labels.first().isEmpty())
        val run = repository.habits.first().single()
        assertNull(run.labelId)
        assertNull(run.labelName)
    }

    @Test
    fun updateHabit_changesNameColorAndLabel_butKeepsHistory() = runBlocking {
        repository.addLabel("Learning")
        val label = repository.labels.first().single()
        repository.addHabit("Read", "#2E7D32")
        val before = repository.habits.first().single()
        repository.toggleToday(before.id)

        assertTrue(repository.updateHabit(before.id, "  Read a book ", "#1565C0", label.id))

        val after = repository.habits.first().single()
        assertEquals("Read a book", after.name)
        assertEquals("#1565C0", after.color)
        assertEquals("Learning", after.labelName)
        assertTrue(after.doneToday)
        assertEquals(1, after.currentStreak)
        assertEquals(before.createdOn, after.createdOn)
    }

    @Test
    fun updateHabit_canClearLabel() = runBlocking {
        repository.addLabel("Fitness")
        val label = repository.labels.first().single()
        repository.addHabit("Run", "#2E7D32", label.id)
        val run = repository.habits.first().single()

        assertTrue(repository.updateHabit(run.id, run.name, run.color, null))

        assertNull(repository.habits.first().single().labelId)
    }

    @Test
    fun updateHabit_rejectsBlankName() = runBlocking {
        repository.addHabit("Read", "#2E7D32")
        val read = repository.habits.first().single()

        assertFalse(repository.updateHabit(read.id, "   ", "#1565C0", null))

        assertEquals("Read", repository.habits.first().single().name)
        assertEquals("#2E7D32", repository.habits.first().single().color)
    }
}
