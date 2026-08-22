package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategoryBudget(
    val category: Category,
    val spent: Double,
    val limit: Double?
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val currentTypeFilter: StateFlow<TransactionType?> = typeFilter.asStateFlow()

    private val currencyContext = combine(displayCurrency, exchangeRateRepository.rates) { display, rates -> display to rates }

    val budgets: StateFlow<List<CategoryBudget>> = combine(typeFilter, currencyContext) { type, currencyPair -> type to currencyPair }
        .flatMapLatest { (type, currencyPair) ->
            val (display, rates) = currencyPair
            val monthStart = LocalDate.now().withDayOfMonth(1)
            val monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
            combine(
                categoryRepository.getByType(type?.let(::categoryTypeFor)),
                transactionRepository.getByTypeAndDateRange(type, monthStart, monthEnd)
            ) { categories, transactions ->
                val spent = spentByCategory(transactions, display, rates)
                categories.map { category ->
                    val convertedLimit = category.budgetLimit?.let { limit ->
                        ExchangeRateRepository.convert(limit, category.budgetLimitCurrency, display, rates)
                    }
                    CategoryBudget(category, spent[category.id] ?: 0.0, convertedLimit)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    companion object {
        // ponytail: pulled out as a pure function so the aggregation is unit-testable without a ViewModel/coroutine scope
        fun spentByCategory(transactions: List<Transaction>, display: Currency, rates: ExchangeRateCache): Map<Long, Double> =
            transactions
                .filter { !it.isIncome }
                .groupBy { it.categoryId }
                .mapValues { (_, txns) -> txns.sumOf { ExchangeRateRepository.convert(it.amount, it.currency, display, rates) } }
    }
}
