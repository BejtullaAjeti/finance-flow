package com.example.financeflow.data

import java.time.LocalDate

fun nextDueDateAfter(rule: RecurringRule): LocalDate = when (rule.frequency) {
    Frequency.WEEKLY -> rule.nextDueDate.plusWeeks(1)
    Frequency.MONTHLY -> rule.nextDueDate.plusMonths(1)
    // ponytail: customIntervalDays isn't enforced at the type level (see RecurringRule); a
    // null/non-positive value is coerced to 1 day so this can't hang the caller in an infinite loop.
    Frequency.CUSTOM -> rule.nextDueDate.plusDays((rule.customIntervalDays ?: 1).coerceAtLeast(1).toLong())
}

data class DueOccurrence(val dueDate: LocalDate, val updatedRule: RecurringRule)

/**
 * Rolls [rule] forward one occurrence at a time for every due date on or before [today] —
 * catching up several missed periods if the app wasn't opened in a while. Each entry pairs the
 * due date (to generate a Transaction for) with the rule as it should be persisted once that
 * occurrence is applied; the last entry's [DueOccurrence.updatedRule] is the final state to save.
 */
fun dueOccurrences(rule: RecurringRule, today: LocalDate): List<DueOccurrence> {
    val occurrences = mutableListOf<DueOccurrence>()
    var current = rule
    while (current.nextDueDate <= today) {
        val dueDate = current.nextDueDate
        current = current.copy(nextDueDate = nextDueDateAfter(current))
        occurrences += DueOccurrence(dueDate, current)
    }
    return occurrences
}
