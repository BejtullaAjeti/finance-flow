package com.example.financeflow.data.repository

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateRepositoryTest {
    private val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)

    @Test
    fun convert_sameCurrency_returnsAmountUnchanged() {
        assertEquals(100.0, ExchangeRateRepository.convert(100.0, Currency.EUR, Currency.EUR, cache), 0.0001)
    }

    @Test
    fun convert_pivotsThroughUsd() {
        // 85 EUR -> USD (85 / 0.85 = 100) -> MKD (100 * 51.0 = 5100)
        assertEquals(5100.0, ExchangeRateRepository.convert(85.0, Currency.EUR, Currency.MKD, cache), 0.01)
    }

    @Test
    fun convert_usdIsIdentityPivot() {
        assertEquals(0.85, ExchangeRateRepository.convert(1.0, Currency.USD, Currency.EUR, cache), 0.0001)
    }
}
