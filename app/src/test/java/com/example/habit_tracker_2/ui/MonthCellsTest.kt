package com.example.habit_tracker_2.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

class MonthCellsTest {

    // September 2026 starts on a Tuesday and has 30 days.
    private val september = YearMonth.of(2026, 9)

    @Test
    fun sundayFirst_padsTwoLeadingDays() {
        val cells = monthCells(september, DayOfWeek.SUNDAY)

        assertEquals(listOf(null, null), cells.take(2))
        assertEquals(september.atDay(1), cells[2])
    }

    @Test
    fun mondayFirst_padsOneLeadingDay() {
        val cells = monthCells(september, DayOfWeek.MONDAY)

        assertEquals(null, cells[0])
        assertEquals(september.atDay(1), cells[1])
    }

    @Test
    fun containsEveryDayInOrder_andFillsWholeWeeks() {
        val cells = monthCells(september, DayOfWeek.SUNDAY)

        assertEquals(35, cells.size)
        assertEquals((1..30).map(september::atDay), cells.filterNotNull())
    }

    @Test
    fun monthStartingOnFirstDayOfWeek_hasNoPadding() {
        // February 2026 starts on a Sunday and has exactly 28 days.
        val february = YearMonth.of(2026, 2)

        val cells = monthCells(february, DayOfWeek.SUNDAY)

        assertEquals(28, cells.size)
        assertEquals(february.atDay(1), cells.first())
        assertEquals(february.atDay(28), cells.last())
    }
}
