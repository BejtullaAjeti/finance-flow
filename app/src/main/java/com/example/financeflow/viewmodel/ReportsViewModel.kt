package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.CategoryTotal
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.PeriodCurrencyTotal
import com.example.financeflow.data.PeriodTotal
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.dateRangeFor
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategorySlice(val category: Category, val total: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val period = MutableStateFlow(ReportPeriod.MONTH)

    val selectedType: StateFlow<TransactionType?> = typeFilter.asStateFlow()
    val selectedPeriod: StateFlow<ReportPeriod> = period.asStateFlow()

    private val selection = combine(typeFilter, period) { type, p -> type to p }
    private val currencyContext = combine(displayCurrency, exchangeRateRepository.rates) { display, rates -> display to rates }

    // Daily has no useful chart per CLAUDE.md — it's a plain list + running total instead.
    val dailyTransactions: StateFlow<List<Transaction>> = selection.flatMapLatest { (type, p) ->
        if (p != ReportPeriod.DAY) return@flatMapLatest flowOf(emptyList())
        val (start, end) = dateRangeFor(p, LocalDate.now())
        transactionRepository.getByTypeAndDateRange(type, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Weekly/Monthly bar charts group by day within their range; Yearly groups by month.
    val barChartData: StateFlow<List<PeriodTotal>> = combine(selection, currencyContext) { s, c -> s to c }
        .flatMapLatest { (selectionPair, currencyPair) ->
            val (type, p) = selectionPair
            val (display, rates) = currencyPair
            val (start, end) = dateRangeFor(p, LocalDate.now())
            val raw = when (p) {
                ReportPeriod.DAY -> flowOf(emptyList())
                ReportPeriod.WEEK, ReportPeriod.MONTH -> transactionRepository.getTotals(type, ReportPeriod.DAY, start, end)
                ReportPeriod.YEAR -> transactionRepository.getTotals(type, ReportPeriod.MONTH, start, end)
            }
            raw.map { foldPeriodTotals(it, display, rates) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryBreakdown: StateFlow<List<CategorySlice>> = combine(selection, currencyContext) { s, c -> s to c }
        .flatMapLatest { (selectionPair, currencyPair) ->
            val (type, p) = selectionPair
            val (display, rates) = currencyPair
            val (start, end) = dateRangeFor(p, LocalDate.now())
            combine(
                transactionRepository.getCategoryTotals(type, start, end),
                categoryRepository.getAll()
            ) { rawTotals, categories ->
                val byId = categories.associateBy { it.id }
                foldCategoryTotals(rawTotals, display, rates).mapNotNull { t ->
                    byId[t.categoryId]?.let { category -> CategorySlice(category, t.total) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    fun setPeriod(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    companion object {
        fun foldPeriodTotals(raw: List<PeriodCurrencyTotal>, display: Currency, rates: ExchangeRateCache): List<PeriodTotal> =
            raw.groupBy { it.bucket }
                .map { (bucket, rows) ->
                    PeriodTotal(
                        bucket = bucket,
                        income = rows.sumOf { ExchangeRateRepository.convert(it.income, it.currency, display, rates) },
                        expense = rows.sumOf { ExchangeRateRepository.convert(it.expense, it.currency, display, rates) }
                    )
                }
                .sortedBy { it.bucket }

        fun foldCategoryTotals(raw: List<CategoryCurrencyTotal>, display: Currency, rates: ExchangeRateCache): List<CategoryTotal> =
            raw.groupBy { it.categoryId }
                .map { (categoryId, rows) ->
                    CategoryTotal(
                        categoryId = categoryId,
                        total = rows.sumOf { ExchangeRateRepository.convert(it.total, it.currency, display, rates) }
                    )
                }
                .sortedByDescending { it.total }
    }
}
