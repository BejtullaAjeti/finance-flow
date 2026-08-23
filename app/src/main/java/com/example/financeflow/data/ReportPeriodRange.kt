package com.example.financeflow.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The (start, end) date range a period covers around referenceDate — the "reusable breakdown
 * query" range half from CLAUDE.md §6: every Reports chart and the category pie derive their
 * window from this, then pick their own grouping (day/month buckets, or a plain list for Daily).
 */
fun dateRangeFor(period: ReportPeriod, referenceDate: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    ReportPeriod.DAY -> referenceDate to referenceDate
    ReportPeriod.WEEK -> {
        val start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        start to start.plusDays(6)
    }
    ReportPeriod.MONTH -> referenceDate.withDayOfMonth(1) to referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
    ReportPeriod.YEAR -> referenceDate.withDayOfYear(1) to referenceDate.withDayOfYear(referenceDate.lengthOfYear())
}
