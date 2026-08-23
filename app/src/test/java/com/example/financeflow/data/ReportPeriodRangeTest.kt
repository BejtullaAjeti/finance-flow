package com.example.financeflow.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportPeriodRangeTest {

    @Test
    fun day_isJustThatDay() {
        val ref = LocalDate.of(2026, 8, 22)
        assertEquals(ref to ref, dateRangeFor(ReportPeriod.DAY, ref))
    }

    @Test
    fun week_isMondayToSunday_evenWhenReferenceIsSunday() {
        // 2026-08-22 is a Saturday; the containing week is Mon 2026-08-17 .. Sun 2026-08-23.
        val (start, end) = dateRangeFor(ReportPeriod.WEEK, LocalDate.of(2026, 8, 22))
        assertEquals(LocalDate.of(2026, 8, 17), start)
        assertEquals(LocalDate.of(2026, 8, 23), end)

        // A Monday reference should be its own week start.
        val (mondayStart, mondayEnd) = dateRangeFor(ReportPeriod.WEEK, LocalDate.of(2026, 8, 17))
        assertEquals(LocalDate.of(2026, 8, 17), mondayStart)
        assertEquals(LocalDate.of(2026, 8, 23), mondayEnd)
    }

    @Test
    fun month_spansFirstToLastDayIncludingLeapFebruary() {
        val (start, end) = dateRangeFor(ReportPeriod.MONTH, LocalDate.of(2026, 8, 22))
        assertEquals(LocalDate.of(2026, 8, 1), start)
        assertEquals(LocalDate.of(2026, 8, 31), end)

        val (febStart, febEnd) = dateRangeFor(ReportPeriod.MONTH, LocalDate.of(2028, 2, 10))
        assertEquals(LocalDate.of(2028, 2, 1), febStart)
        assertEquals(LocalDate.of(2028, 2, 29), febEnd)
    }

    @Test
    fun year_spansJanFirstToDecThirtyFirst() {
        val (start, end) = dateRangeFor(ReportPeriod.YEAR, LocalDate.of(2026, 8, 22))
        assertEquals(LocalDate.of(2026, 1, 1), start)
        assertEquals(LocalDate.of(2026, 12, 31), end)
    }
}
