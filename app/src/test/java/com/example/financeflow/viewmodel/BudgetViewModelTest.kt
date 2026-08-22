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
}
