package com.example.financeflow.viewmodel

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetViewModelTest {

    @Test
    fun spentByCategory_sumsExpensesOnlyAndGroupsByCategory() {
        val transactions = listOf(
            Transaction(amount = 20.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 30.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 15.0, type = TransactionType.PERSONAL, categoryId = 2, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 500.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true)
        )

        val result = BudgetViewModel.spentByCategory(transactions, Currency.MKD, ExchangeRateCache())

        assertEquals(50.0, result[1L])
        assertEquals(15.0, result[2L])
    }

    @Test
    fun spentByCategory_convertsEachTransactionToDisplayCurrency() {
        val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)
        val transactions = listOf(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false, currency = Currency.USD)
        )

        val result = BudgetViewModel.spentByCategory(transactions, Currency.MKD, cache)

        assertEquals(5100.0, result[1L]!!, 0.01)
    }

    @Test
    fun spentForCategory_onlyCountsTransactionsInsideTheGivenRangeForThatCategory() {
        val transactions = listOf(
            // In range, category 1
            Transaction(amount = 10.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.of(2026, 3, 15), isIncome = false),
            // Out of range (before), category 1
            Transaction(amount = 999.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.of(2026, 2, 28), isIncome = false),
            // Out of range (after), category 1
            Transaction(amount = 999.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.of(2026, 4, 1), isIncome = false),
            // In range but wrong category
            Transaction(amount = 999.0, type = TransactionType.PERSONAL, categoryId = 2, date = LocalDate.of(2026, 3, 15), isIncome = false),
            // In range and right category, but income
            Transaction(amount = 999.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.of(2026, 3, 15), isIncome = true)
        )
        val march = LocalDate.of(2026, 3, 1) to LocalDate.of(2026, 3, 31)

        val result = BudgetViewModel.spentForCategory(transactions, categoryId = 1, dateRange = march, display = Currency.MKD, rates = ExchangeRateCache())

        assertEquals(10.0, result, 0.01)
    }

    @Test
    fun spentForCategory_convertsToDisplayCurrency() {
        val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)
        val transactions = listOf(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.of(2026, 3, 15), isIncome = false, currency = Currency.USD)
        )
        val march = LocalDate.of(2026, 3, 1) to LocalDate.of(2026, 3, 31)

        val result = BudgetViewModel.spentForCategory(transactions, categoryId = 1, dateRange = march, display = Currency.MKD, rates = cache)

        assertEquals(5100.0, result, 0.01)
    }
}
