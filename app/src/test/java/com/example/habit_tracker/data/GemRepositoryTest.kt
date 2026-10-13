package com.jaspermsnbk.habit_tracker.data

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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GemRepositoryTest {

    private val born = LocalDate.of(2026, 10, 1)
    private lateinit var db: HabitDatabase
    private lateinit var gems: GemRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        gems = GemRepository(db.gemDao())
    }

    @After
    fun tearDown() = db.close()

    private fun balance() = runBlocking { gems.balance.first() }

    @Test
    fun balanceStartsAtZero_andIsTheSumOfTheLedger() = runBlocking {
        assertEquals(0, balance())

        gems.awardStreakGems(born, run = 0, streak = 14)

        assertEquals(15, balance()) // 5 for day 7, 10 for day 14
    }

    @Test
    fun aMilestonePaysOnce_evenIfAwardedAgain() = runBlocking {
        assertEquals(5, gems.awardStreakGems(born, 0, 7))
        assertEquals(0, gems.awardStreakGems(born, 0, 7))
        assertEquals(0, gems.awardStreakGems(born, 0, 8))
        assertEquals(10, gems.awardStreakGems(born, 0, 14))

        assertEquals(15, balance())
    }

    @Test
    fun aBrokenStreak_canEarnTheSameMilestoneAgain_andSoCanAReplacementPet() = runBlocking {
        gems.awardStreakGems(born, run = 0, streak = 7)
        gems.awardStreakGems(born, run = 1, streak = 7)
        gems.awardStreakGems(born.plusDays(30), run = 0, streak = 7)

        assertEquals(15, balance())
    }

    @Test
    fun buying_spendsGemsAndOwnsTheItem() = runBlocking {
        gems.awardStreakGems(born, 0, 30) // 5 + 10 + 25 = 40

        assertEquals(BuyResult.Bought, gems.buy("outfit-runner", 30))

        assertEquals(10, balance())
        assertEquals(setOf("outfit-runner"), gems.owned.first())
    }

    @Test
    fun buying_withTooFewGems_changesNothing() = runBlocking {
        gems.awardStreakGems(born, 0, 7) // 5

        assertEquals(BuyResult.NotEnoughGems(missing = 25), gems.buy("outfit-runner", 30))

        assertEquals(5, balance())
        assertTrue(gems.owned.first().isEmpty())
    }

    @Test
    fun buyingTwice_isRefused_andChargesOnce() = runBlocking {
        gems.awardStreakGems(born, 0, 60) // 90 gems

        assertEquals(BuyResult.Bought, gems.buy("outfit-runner", 30))
        assertEquals(BuyResult.AlreadyOwned, gems.buy("outfit-runner", 30))

        assertEquals(60, balance())
    }

    @Test
    fun equipping_needsOwnership_replacesTheSlot_andCanBeUndone() = runBlocking {
        gems.awardStreakGems(born, 0, 60)
        assertFalse(gems.equip("outfit-runner", "Outfit"))

        gems.buy("outfit-runner", 30)
        gems.buy("outfit-reader", 30)
        assertTrue(gems.equip("outfit-runner", "Outfit"))
        assertEquals(mapOf("Outfit" to "outfit-runner"), gems.equipped.first())

        assertTrue(gems.equip("outfit-reader", "Outfit"))
        assertEquals(mapOf("Outfit" to "outfit-reader"), gems.equipped.first())

        gems.unequip("Outfit")
        assertTrue(gems.equipped.first().isEmpty())
    }

    @Test
    fun grantItem_ownsWithoutCharging() = runBlocking {
        gems.grantItem("outfit-mindful")

        assertEquals(0, balance())
        assertEquals(setOf("outfit-mindful"), gems.owned.first())
    }

    @Test
    fun aPurchase_creditsOnce_evenIfDeliveredTwice() = runBlocking {
        assertTrue(gems.recordPurchase("order-1", 100))
        assertFalse(gems.recordPurchase("order-1", 100))

        assertEquals(100, balance())
    }

    @Test
    fun aRefund_takesTheGemsBack_once() = runBlocking {
        gems.recordPurchase("order-1", 100)

        assertTrue(gems.recordRefund("order-1"))
        assertFalse(gems.recordRefund("order-1"))
        assertFalse(gems.recordRefund("unknown"))

        assertEquals(0, balance())
    }

    @Test
    fun deleteAll_clearsGemsItemsAndWhatIsWorn() = runBlocking {
        gems.awardStreakGems(born, 0, 30)
        gems.buy("outfit-runner", 30)
        gems.equip("outfit-runner", "Outfit")

        gems.deleteAll()

        assertEquals(0, balance())
        assertTrue(gems.owned.first().isEmpty())
        assertTrue(gems.equipped.first().isEmpty())
    }
}
