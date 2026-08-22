package com.example.financeflow.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun localDateRoundTrips() {
        val date = LocalDate.of(2026, 8, 17)
        assertEquals(date, converters.fromEpochDay(converters.toEpochDay(date)))
    }

    @Test
    fun enumsRoundTrip() {
        assertEquals(TransactionType.BUSINESS, converters.fromTransactionType(converters.toTransactionType(TransactionType.BUSINESS)))
        assertEquals(CategoryType.BOTH, converters.fromCategoryType(converters.toCategoryType(CategoryType.BOTH)))
        assertEquals(Frequency.CUSTOM, converters.fromFrequency(converters.toFrequency(Frequency.CUSTOM)))
        assertEquals(Currency.EUR, converters.fromCurrency(converters.toCurrency(Currency.EUR)))
    }
}
