package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
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
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionListFilter(
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dateRange = MutableStateFlow(
        LocalDate.now().withDayOfMonth(1) to LocalDate.now()
    )

    val transactionsInRange: StateFlow<List<Transaction>> = dateRange
        .flatMapLatest { (start, end) -> repository.getByDateRange(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDateRange(start: LocalDate, end: LocalDate) {
        dateRange.value = start to end
    }

    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val currentTypeFilter: StateFlow<TransactionType?> = typeFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(dateRange, typeFilter) { range, type ->
        Triple(range.first, range.second, type)
    }.flatMapLatest { (start, end, type) ->
        repository.getByTypeAndDateRange(type, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    private val listFilter = MutableStateFlow(TransactionListFilter())

    val currentListFilter: StateFlow<TransactionListFilter> = listFilter.asStateFlow()

    val filteredList: StateFlow<List<Transaction>> = listFilter.flatMapLatest { f ->
        repository.getFiltered(f.type, f.categoryId, f.startDate, f.endDate, f.searchQuery.ifBlank { null })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateListFilter(transform: (TransactionListFilter) -> TransactionListFilter) {
        listFilter.value = transform(listFilter.value)
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.insert(transaction) }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.update(transaction) }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction) }
    }

    data class MonthSummary(val income: Double, val expense: Double)

    val monthSummary: StateFlow<MonthSummary> = combine(filteredTransactions, exchangeRateRepository.rates, displayCurrency) { txns, rates, display ->
        MonthSummary(
            income = sumConverted(txns.filter { it.isIncome }, display, rates),
            expense = sumConverted(txns.filter { !it.isIncome }, display, rates)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthSummary(0.0, 0.0))

    companion object {
        fun sumConverted(transactions: List<Transaction>, display: Currency, rates: ExchangeRateCache): Double =
            transactions.sumOf { ExchangeRateRepository.convert(it.amount, it.currency, display, rates) }
    }
}
