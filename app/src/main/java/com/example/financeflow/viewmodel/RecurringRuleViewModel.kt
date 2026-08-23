package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.repository.RecurringRuleRepository
import com.example.financeflow.recurring.RecurringRuleProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringRuleViewModel(
    private val repository: RecurringRuleRepository,
    private val processor: RecurringRuleProcessor
) : ViewModel() {

    val rules: StateFlow<List<RecurringRule>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val currentTypeFilter: StateFlow<TransactionType?> = typeFilter.asStateFlow()

    val filteredRules: StateFlow<List<RecurringRule>> = typeFilter
        .flatMapLatest { repository.getByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    // The rule may already be due (nextDueDate <= today) the moment it's saved — generating here
    // instead of waiting for the next app launch or 12-hourly WorkManager tick is what makes
    // adding/resuming a rule visibly do something right away.
    fun addRule(rule: RecurringRule) {
        viewModelScope.launch {
            repository.insert(rule)
            processor.generateDueTransactions()
        }
    }

    fun updateRule(rule: RecurringRule) {
        viewModelScope.launch {
            repository.update(rule)
            processor.generateDueTransactions()
        }
    }

    fun deleteRule(rule: RecurringRule) {
        viewModelScope.launch { repository.delete(rule) }
    }
}
