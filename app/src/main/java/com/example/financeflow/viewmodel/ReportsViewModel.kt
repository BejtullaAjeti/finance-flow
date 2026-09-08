package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.CategoryTotal
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.ReportPeriod
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
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategorySlice(val category: Category, val income: Double, val expense: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val period = MutableStateFlow(ReportPeriod.MONTH)
    // Only meaningful when period == DAY — the "pick any specific day" reference date. Other
    // periods always anchor on today's current week/month/year, same as before.
    private val dailyDate = MutableStateFlow(LocalDate.now())

    val selectedType: StateFlow<TransactionType?> = typeFilter.asStateFlow()
    val selectedPeriod: StateFlow<ReportPeriod> = period.asStateFlow()
    val selectedDailyDate: StateFlow<LocalDate> = dailyDate.asStateFlow()

    private val selection = combine(typeFilter, period, dailyDate) { type, p, d -> Triple(type, p, d) }
    private val currencyContext = combine(displayCurrency, exchangeRateRepository.rates) { display, rates -> display to rates }

    // Income and expense per category for the selected (type, period, referenceDate) — the single
    // source Reports builds its totals, stacked bars, and category list from.
    val categoryBreakdown: StateFlow<List<CategorySlice>> = combine(selection, currencyContext) { s, c -> s to c }
        .flatMapLatest { (selectionTriple, currencyPair) ->
            val (type, p, dailyRefDate) = selectionTriple
            val (display, rates) = currencyPair
            val referenceDate = if (p == ReportPeriod.DAY) dailyRefDate else LocalDate.now()
            val (start, end) = dateRangeFor(p, referenceDate)
            combine(
                transactionRepository.getCategoryTotals(type, start, end),
                categoryRepository.getAll()
            ) { rawTotals, categories ->
                val byId = categories.associateBy { it.id }
                foldCategoryTotals(rawTotals, display, rates).mapNotNull { t ->
                    byId[t.categoryId]?.let { category -> CategorySlice(category, t.income, t.expense) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    fun setPeriod(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    fun setDailyDate(date: LocalDate) {
        dailyDate.value = date
    }

    companion object {
        fun foldCategoryTotals(raw: List<CategoryCurrencyTotal>, display: Currency, rates: ExchangeRateCache): List<CategoryTotal> =
            raw.groupBy { it.categoryId }
                .map { (categoryId, rows) ->
                    CategoryTotal(
                        categoryId = categoryId,
                        income = rows.sumOf { ExchangeRateRepository.convert(it.income, it.currency, display, rates) },
                        expense = rows.sumOf { ExchangeRateRepository.convert(it.expense, it.currency, display, rates) }
                    )
                }
    }
}
