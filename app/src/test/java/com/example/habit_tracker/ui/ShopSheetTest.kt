package com.jaspermsnbk.habit_tracker.ui

import android.app.Activity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaspermsnbk.habit_tracker.data.GemPack
import com.jaspermsnbk.habit_tracker.data.GemPurchaseProvider
import com.jaspermsnbk.habit_tracker.data.GemRepository
import com.jaspermsnbk.habit_tracker.data.HabitDatabase
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.PurchaseResult
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
import java.time.LocalDate

// A tall screen, so the whole sheet is on screen at once.
@Config(qualifiers = "w400dp-h1600dp")
@RunWith(AndroidJUnit4::class)
class ShopSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HabitDatabase
    private lateinit var gems: GemRepository

    private val born = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        gems = GemRepository(db.gemDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launch(purchases: GemPurchaseProvider = com.jaspermsnbk.habit_tracker.data.NoGemPurchases) {
        val viewModel = HabitViewModel(HabitRepository(db.habitDao()), testPreferences(), gems)
        val shop = ShopViewModel(gems, testPreferences(), purchases)
        composeRule.setContent { ProfileScreen(viewModel, shop = shop) }
    }

    private fun openShop() {
        composeRule.onNodeWithContentDescription("Shop").performClick()
    }

    private fun balance() = runBlocking { gems.balance.first() }

    private fun waitForText(text: String) = composeRule.waitUntilSettled {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun theShopButton_opensTheSheet_withTheBalanceAndTheOutfits() {
        runBlocking { gems.awardStreakGems(born, 0, 14) } // 15 gems
        launch()

        openShop()

        waitForText("Runner gear")
        composeRule.onNodeWithText("Reader's glasses").assertIsDisplayed()
        composeRule.onNodeWithText("Calm aura").assertIsDisplayed()
        composeRule.waitUntilSettled {
            composeRule.onAllNodesWithContentDescription("15 gems").fetchSemanticsNodes().size >= 2
        } // the profile header and the sheet
    }

    @Test
    fun anItemYouCannotAffordYet_saysHowManyMoreGemsItNeeds() {
        runBlocking { gems.awardStreakGems(born, 0, 7) } // 5 gems
        launch()
        openShop()
        waitForText("Runner gear")

        composeRule.onNodeWithText("Runner gear").performClick()

        composeRule.onNodeWithText("Buy for 30").assertIsNotEnabled()
        composeRule.onNodeWithText("25 more gems needed. Streaks earn them.").assertIsDisplayed()
    }

    @Test
    fun buyingAnItem_spendsTheGems_andPutsItOn() {
        runBlocking { gems.awardStreakGems(born, 0, 30) } // 40 gems
        launch()
        openShop()
        waitForText("Runner gear")
        composeRule.onNodeWithText("Runner gear").performClick()

        composeRule.onNodeWithText("Buy for 30").assertIsEnabled().performClick()

        composeRule.waitUntilSettled { balance() == 10 }
        waitForText("Take off")
        composeRule.onNodeWithText("Runner gear is yours, and your pet is wearing it.").assertIsDisplayed()
        assertEquals(setOf("outfit-runner"), runBlocking { gems.owned.first() })
        assertEquals(mapOf("Outfit" to "outfit-runner"), runBlocking { gems.equipped.first() })
    }

    @Test
    fun anOwnedItem_canBeTakenOffAndWornAgain() {
        runBlocking { gems.grantItem("outfit-reader") }
        launch()
        openShop()
        waitForText("Owned")
        composeRule.onNodeWithText("Reader's glasses").performClick()

        composeRule.onNodeWithText("Wear").performClick()
        waitForText("Take off")
        assertEquals(mapOf("Outfit" to "outfit-reader"), runBlocking { gems.equipped.first() })

        composeRule.onNodeWithText("Take off").performClick()
        waitForText("Wear")
        assertEquals(emptyMap<String, String>(), runBlocking { gems.equipped.first() })
    }

    @Test
    fun backReturnsToTheList() {
        launch()
        openShop()
        waitForText("Calm aura")
        composeRule.onNodeWithText("Calm aura").performClick()

        composeRule.onNodeWithText("Back to the shop").performClick()

        composeRule.onNodeWithText("Calm aura").assertIsDisplayed()
        composeRule.onNodeWithText("Back to the shop").assertDoesNotExist()
    }

    @Test
    fun withoutAStore_thereIsNoGetGemsRow() {
        launch()
        openShop()
        waitForText("Runner gear")

        composeRule.onNodeWithText("Get gems").assertDoesNotExist()
    }

    @Test
    fun withAStore_packsAreOffered_andAPurchaseDeliveredTwiceCreditsOnce() {
        val store = object : GemPurchaseProvider {
            override val available = true
            override suspend fun packs() = listOf(GemPack("gems-100", 100, "\$0.99"))
            override suspend fun buy(activity: Activity, pack: GemPack): PurchaseResult =
                PurchaseResult.Success(orderId = "order-1", gems = pack.gems)
        }
        launch(store)
        openShop()
        waitForText("Get gems")

        val pack = hasText("100 gems · \$0.99") and hasClickAction()
        composeRule.onNode(pack).performClick()
        composeRule.waitUntilSettled { balance() == 100 }
        composeRule.onNode(pack).performClick()
        Thread.sleep(200)
        composeRule.waitForIdle()

        assertEquals(100, balance())
    }
}
