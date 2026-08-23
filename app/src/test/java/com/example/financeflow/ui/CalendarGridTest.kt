package com.example.financeflow.ui

import com.example.financeflow.ui.components.calendarGridStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    @Test
    fun `August 2026 starting Monday begins on the last Monday of July`() {
        // Aug 1 2026 is a Saturday; week starting Monday needs 5 leading days from July.
        val start = calendarGridStart(YearMonth.of(2026, 8), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 7, 27), start)
    }

    @Test
    fun `month starting exactly on the week's first day has no leading days`() {
        // Jun 1 2026 is a Monday.
        val start = calendarGridStart(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 6, 1), start)
    }

    @Test
    fun `Sunday-first week for a month starting on Sunday`() {
        // Nov 1 2026 is a Sunday.
        val start = calendarGridStart(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), start)
    }
}
