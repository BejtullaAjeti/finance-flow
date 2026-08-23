package com.example.financeflow.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private fun rule(
    frequency: Frequency,
    nextDueDate: LocalDate,
    customIntervalDays: Int? = null,
    active: Boolean = true
) = RecurringRule(
    id = 1,
    label = "Rent",
    amount = 100.0,
    categoryId = 1,
    type = TransactionType.PERSONAL,
    isIncome = false,
    frequency = frequency,
    customIntervalDays = customIntervalDays,
    nextDueDate = nextDueDate,
    active = active
)

class RecurringRuleGenerationTest {

    @Test
    fun nextDueDateAfter_advancesByFrequency() {
        assertEquals(
            LocalDate.of(2026, 8, 29),
            nextDueDateAfter(rule(Frequency.WEEKLY, LocalDate.of(2026, 8, 22)))
        )
        assertEquals(
            LocalDate.of(2026, 9, 22),
            nextDueDateAfter(rule(Frequency.MONTHLY, LocalDate.of(2026, 8, 22)))
        )
        assertEquals(
            LocalDate.of(2026, 9, 1),
            nextDueDateAfter(rule(Frequency.CUSTOM, LocalDate.of(2026, 8, 22), customIntervalDays = 10))
        )
    }

    @Test
    fun nextDueDateAfter_customWithNullOrZeroInterval_fallsBackToOneDay() {
        // A missing or non-positive customIntervalDays must never stall the catch-up loop.
        assertEquals(
            LocalDate.of(2026, 8, 23),
            nextDueDateAfter(rule(Frequency.CUSTOM, LocalDate.of(2026, 8, 22), customIntervalDays = null))
        )
        assertEquals(
            LocalDate.of(2026, 8, 23),
            nextDueDateAfter(rule(Frequency.CUSTOM, LocalDate.of(2026, 8, 22), customIntervalDays = 0))
        )
        assertEquals(
            LocalDate.of(2026, 8, 23),
            nextDueDateAfter(rule(Frequency.CUSTOM, LocalDate.of(2026, 8, 22), customIntervalDays = -5))
        )
    }

    @Test
    fun dueOccurrences_returnsEmptyWhenNotYetDue() {
        val notYetDue = rule(Frequency.MONTHLY, LocalDate.of(2026, 9, 1))
        assertTrue(dueOccurrences(notYetDue, LocalDate.of(2026, 8, 22)).isEmpty())
    }

    @Test
    fun dueOccurrences_singleOccurrence_advancesOnce() {
        val due = rule(Frequency.WEEKLY, LocalDate.of(2026, 8, 22))
        val occurrences = dueOccurrences(due, LocalDate.of(2026, 8, 22))

        assertEquals(1, occurrences.size)
        assertEquals(LocalDate.of(2026, 8, 22), occurrences[0].dueDate)
        assertEquals(LocalDate.of(2026, 8, 29), occurrences[0].updatedRule.nextDueDate)
    }

    @Test
    fun dueOccurrences_catchesUpMultipleMissedPeriods() {
        // App wasn't opened for a month; a weekly rule due 2026-08-01 should generate 4
        // occurrences by 2026-08-22, ending with nextDueDate pushed into the future.
        val staleRule = rule(Frequency.WEEKLY, LocalDate.of(2026, 8, 1))
        val today = LocalDate.of(2026, 8, 22)

        val occurrences = dueOccurrences(staleRule, today)

        assertEquals(4, occurrences.size)
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 8),
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 8, 22)
            ),
            occurrences.map { it.dueDate }
        )
        val finalNextDueDate = occurrences.last().updatedRule.nextDueDate
        assertEquals(LocalDate.of(2026, 8, 29), finalNextDueDate)
        assertTrue(finalNextDueDate > today)
    }
}
