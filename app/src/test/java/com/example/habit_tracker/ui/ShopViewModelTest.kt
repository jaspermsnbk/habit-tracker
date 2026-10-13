package com.jaspermsnbk.habit_tracker.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.GemRepository
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.inMemoryDatabase
import com.jaspermsnbk.habit_tracker.data.testPreferences
import com.jaspermsnbk.habit_tracker.ui.pet.Outfit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ShopViewModelTest {

    private lateinit var db: HabitDatabase
    private lateinit var gems: GemRepository
    private val born = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        gems = GemRepository(db.gemDao())
    }

    @After
    fun tearDown() = db.close()

    private fun shop() = ShopViewModel(gems, testPreferences())

    /** Keeps the view model's flows running, as a screen would, and waits for [condition]. */
    private fun ShopViewModel.watching(condition: ShopViewModel.() -> Boolean) {
        val jobs = listOf(
            balance.onEach { }.launchIn(kotlinx.coroutines.MainScope()),
            unseenGems.onEach { }.launchIn(kotlinx.coroutines.MainScope()),
            outfit.onEach { }.launchIn(kotlinx.coroutines.MainScope()),
        )
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting" }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
        }
        jobs.forEach { it.cancel() }
    }

    @Test
    fun newGems_areUnseenUntilDismissed() {
        runBlocking { gems.awardStreakGems(born, 0, 14) }
        val shop = shop()

        shop.watching { unseenGems.value == 15 }
        shop.markGemsSeen()
        shop.watching { unseenGems.value == 0 }

        // A later streak pays again, and only that is new.
        runBlocking {
            Thread.sleep(5)
            gems.awardStreakGems(born, 0, 30)
        }
        shop.watching { unseenGems.value == 25 }
    }

    @Test
    fun buyingAnOutfit_wearsIt_andTheOutfitFlowFollows() {
        runBlocking { gems.awardStreakGems(born, 0, 30) }
        val shop = shop()
        shop.watching { balance.value == 40 }

        shop.buy(com.jaspermsnbk.habit_tracker.ui.pet.ShopCatalog.find("outfit-mindful")!!)

        shop.watching { outfit.value == Outfit.Mindful }
        assertEquals(10, runBlocking { gems.balance.first() })
    }
}
