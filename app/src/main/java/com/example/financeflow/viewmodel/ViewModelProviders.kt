package com.example.financeflow.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.data.repository.RecurringRuleRepository
import com.example.financeflow.data.repository.TransactionRepository
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.recurring.RecurringRuleProcessor

@Composable
fun rememberTransactionViewModel(): TransactionViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember { TransactionRepository(AppDatabase.getInstance(context).transactionDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { TransactionViewModel(repository, exchangeRateRepository, displayCurrency) } })
}

@Composable
fun rememberCategoryViewModel(): CategoryViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember { CategoryRepository(AppDatabase.getInstance(context).categoryDao()) }
    return viewModel(factory = viewModelFactory { initializer { CategoryViewModel(repository) } })
}

@Composable
fun rememberBudgetViewModel(): BudgetViewModel {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    val categoryRepository = remember { CategoryRepository(db.categoryDao()) }
    val transactionRepository = remember { TransactionRepository(db.transactionDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(db.exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { BudgetViewModel(categoryRepository, transactionRepository, exchangeRateRepository, displayCurrency) } })
}

@Composable
fun rememberRecurringRuleViewModel(): RecurringRuleViewModel {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { RecurringRuleRepository(db.recurringRuleDao()) }
    val processor = remember { RecurringRuleProcessor(db) }
    return viewModel(factory = viewModelFactory { initializer { RecurringRuleViewModel(repository, processor) } })
}

@Composable
fun rememberReportsViewModel(): ReportsViewModel {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    val transactionRepository = remember { TransactionRepository(db.transactionDao()) }
    val categoryRepository = remember { CategoryRepository(db.categoryDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(db.exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { ReportsViewModel(transactionRepository, categoryRepository, exchangeRateRepository, displayCurrency) } })
}
