package com.example.financeflow.viewmodel

import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.PeriodCurrencyTotal
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsViewModelFoldTest {
    private val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)

    @Test
    fun foldPeriodTotals_convertsAndSumsAcrossCurrenciesPerBucket() {
        val raw = listOf(
            PeriodCurrencyTotal(bucket = "2026-08-01", currency = Currency.USD, income = 100.0, expense = 0.0),
            PeriodCurrencyTotal(bucket = "2026-08-01", currency = Currency.EUR, income = 0.0, expense = 85.0)
        )

        val folded = ReportsViewModel.foldPeriodTotals(raw, Currency.MKD, cache)

        assertEquals(1, folded.size)
        // 100 USD -> MKD = 5100; 85 EUR -> USD (100) -> MKD = 5100
        assertEquals(5100.0, folded[0].income, 0.01)
        assertEquals(5100.0, folded[0].expense, 0.01)
    }

    @Test
    fun foldCategoryTotals_convertsAndSumsAcrossCurrenciesPerCategory() {
        val raw = listOf(
            CategoryCurrencyTotal(categoryId = 1, currency = Currency.USD, total = 100.0),
            CategoryCurrencyTotal(categoryId = 1, currency = Currency.EUR, total = 85.0)
        )

        val folded = ReportsViewModel.foldCategoryTotals(raw, Currency.MKD, cache)

        assertEquals(1, folded.size)
        // 100 USD -> MKD (5100) + 85 EUR -> USD (100) -> MKD (5100) = 10200
        assertEquals(10200.0, folded[0].total, 0.01)
    }
}
