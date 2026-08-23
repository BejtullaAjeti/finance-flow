package com.example.financeflow.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Plus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.TransactionRow
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.ui.theme.MoneyFigure
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.TransactionViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberTransactionViewModel

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val context = LocalContext.current
    val transactions by transactionViewModel.filteredTransactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }

    val typeFilter by transactionViewModel.currentTypeFilter.collectAsState()
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
    val currencyFormat = rememberCurrencyFormat(displayCurrency)
    val dateFormat = rememberDateFormat("MMM d")
    val uncategorized = stringResource(R.string.category_uncategorized)

    val summary by transactionViewModel.monthSummary.collectAsState()

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = onAddTransaction, contentDescription = stringResource(R.string.home_add_transaction_content_description)) {
                Icon(PhosphorIcons.Regular.Plus, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TransactionTypeToggle(
                selected = typeFilter,
                onSelect = transactionViewModel::setTypeFilter
            )

            Spacer(Modifier.height(16.dp))

            Crossfade(targetState = typeFilter, label = "homeTypeFilterContent") { _ ->
                Column {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.home_this_month_title), style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.toggle_income), style = MaterialTheme.typography.bodyMedium)
                            Text(text = currencyFormat.format(summary.income), style = MoneyFigure, color = Income)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.home_expenses_label), style = MaterialTheme.typography.bodyMedium)
                            Text(text = currencyFormat.format(summary.expense), style = MoneyFigure, color = Expense)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(text = stringResource(R.string.home_recent_transactions_title), style = MaterialTheme.typography.titleLarge)

                    Spacer(Modifier.height(8.dp))

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.transactions_empty), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(transactions.take(5), key = { it.id }) { transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    categoryName = categoryNames[transaction.categoryId] ?: uncategorized,
                                    displayAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates),
                                    currencyFormat = currencyFormat,
                                    dateFormat = dateFormat,
                                    displayCurrency = displayCurrency,
                                    onClick = { onEditTransaction(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
