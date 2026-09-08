package com.example.financeflow.data.backup

import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupSerializerTest {

    @Test
    fun roundTrip_preservesAllFieldsIncludingNulls() {
        val payload = BackupPayload(
            categories = listOf(
                Category(id = 1, name = "Groceries", type = CategoryType.PERSONAL, budgetLimit = 500.0, budgetLimitCurrency = Currency.EUR, icon = "food", color = "#7FB88F"),
                Category(id = 2, name = "Bank Fees", type = CategoryType.BOTH, budgetLimit = null, icon = null, color = null),
                Category(id = 3, name = "Ushqim", nameKey = "food", type = CategoryType.PERSONAL, icon = "food")
            ),
            recurringRules = listOf(
                RecurringRule(
                    id = 1, label = "Rent", amount = 500.0, currency = Currency.USD, categoryId = 1, type = TransactionType.PERSONAL,
                    isIncome = false, frequency = Frequency.MONTHLY, customIntervalDays = null,
                    nextDueDate = LocalDate.of(2026, 9, 1), active = true
                ),
                RecurringRule(
                    id = 2, label = "Every 10 days", amount = 15.0, categoryId = 2, type = TransactionType.BUSINESS,
                    isIncome = false, frequency = Frequency.CUSTOM, customIntervalDays = 10,
                    nextDueDate = LocalDate.of(2026, 9, 5), active = false
                )
            ),
            transactions = listOf(
                Transaction(
                    id = 1, amount = 42.5, currency = Currency.CHF, type = TransactionType.PERSONAL, categoryId = 1,
                    date = LocalDate.of(2026, 8, 1), note = "milk and eggs", isIncome = false, recurringId = null
                ),
                Transaction(
                    id = 2, amount = 500.0, type = TransactionType.PERSONAL, categoryId = 1,
                    date = LocalDate.of(2026, 9, 1), note = null, isIncome = false, recurringId = 1
                )
            )
        )

        val restored = parseBackup(payload.toJson())

        assertEquals(payload, restored)
    }

    @Test
    fun roundTrip_handlesEmptyLists() {
        val payload = BackupPayload(emptyList(), emptyList(), emptyList())
        assertEquals(payload, parseBackup(payload.toJson()))
    }
}
