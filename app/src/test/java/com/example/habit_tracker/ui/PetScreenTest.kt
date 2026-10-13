package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitEntity
import com.jaspermsnbk.habit_tracker.data.HabitEntryEntity
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.PetEntity
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import com.jaspermsnbk.habit_tracker.data.waitUntilSettled
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

// A tall screen, so the whole pet tab is on screen at once.
@Config(qualifiers = "w400dp-h1400dp")
@RunWith(AndroidJUnit4::class)
class PetScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = HabitRepository(db.habitDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launch() {
        val viewModel = HabitViewModel(repository, testPreferences())
        composeRule.setContent { PetScreen(viewModel) }
    }

    /** A pet born [daysAgo] days ago with a habit done on each of the last [fedDays] days (counting today). */
    private fun seedPet(name: String, species: String, daysAgo: Long, fedDays: Int) = runBlocking {
        val dao = db.habitDao()
        dao.upsertPet(PetEntity(name = name, bornOn = today.minusDays(daysAgo), species = species))
        val created = Instant.now().minusSeconds(86_400L * (daysAgo + 1))
        dao.upsertHabit(HabitEntity(id = "h", name = "Read", color = "#2E7D32", createdAt = created, updatedAt = created))
        for (offset in 0 until fedDays) {
            dao.insertEntry(HabitEntryEntity("e$offset", "h", today.minusDays(offset.toLong()), Instant.now()))
        }
    }

    @Test
    fun withoutAPet_offersTheSpecies_andAdoptNeedsAName() {
        launch()

        composeRule.onNodeWithText("Choose your pet").assertIsDisplayed()
        listOf("Mochi", "Pip", "Bun", "Shell").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Adopt").assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextInput("Momo")

        composeRule.onNodeWithText("Adopt").assertIsEnabled()
    }

    @Test
    fun adopting_savesTheChosenSpeciesAndName() {
        launch()

        composeRule.onNodeWithText("Pip").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("  Sunny ")
        composeRule.onNodeWithText("Adopt").performClick()

        composeRule.waitUntilSettled { runBlocking { repository.pet.first() } != null }
        val pet = runBlocking { repository.pet.first()!! }
        assertEquals("Sunny", pet.name)
        assertEquals("Pip", pet.species)
        // A new pet is an egg until its first fed day.
        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Level 1 · Egg").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aGrownPet_showsItsLevelFormAndWhatIsLeftToEvolve() {
        seedPet("Momo", "Bun", daysAgo = 9, fedDays = 8)

        launch()

        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Momo").fetchSemanticsNodes().isNotEmpty() }
        // 8 fed days: stage 2 (Bun), level 1 + 8 / 3 = 3, and 13 days short of 21.
        composeRule.onNodeWithText("Level 3 · Bun").assertIsDisplayed()
        composeRule.onNodeWithText("13 more fed days to become Grand Bun").assertIsDisplayed()
    }

    @Test
    fun aFullyGrownPet_saysSo() {
        seedPet("Momo", "Shell", daysAgo = 21, fedDays = 22)

        launch()

        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Momo").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Fully grown").assertIsDisplayed()
        composeRule.onNodeWithText("Level 8 · Elder Shell").assertIsDisplayed()
    }

    @Test
    fun aDeadPet_canBeReplaced_andTheChoiceCanBeCancelled() {
        seedPet("Momo", "Mochi", daysAgo = 8, fedDays = 0)

        launch()

        composeRule.waitUntilSettled { composeRule.onAllNodesWithText("Adopt a new pet").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Adopt a new pet").performClick()
        composeRule.onNodeWithText("Choose your pet").assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Choose your pet").assertDoesNotExist()
        composeRule.onNodeWithText("Adopt a new pet").assertIsDisplayed()
    }
}
