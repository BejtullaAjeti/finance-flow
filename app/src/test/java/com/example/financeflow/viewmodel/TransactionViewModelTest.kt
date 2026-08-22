package com.example.financeflow.viewmodel

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionViewModelTest {
    @Test
    fun sumConverted_convertsEachTransactionBeforeSumming() {
        val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)
        val transactions = listOf(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true, currency = Currency.USD),
            Transaction(amount = 85.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true, currency = Currency.EUR)
        )

        val result = TransactionViewModel.sumConverted(transactions, Currency.MKD, cache)

        // 100 USD -> MKD (5100) + 85 EUR -> USD (100) -> MKD (5100) = 10200
        assertEquals(10200.0, result, 0.01)
    }
}
