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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.GlassRow
import com.example.financeflow.ui.components.TransactionRow
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.theme.MoneyFigure
import com.example.financeflow.ui.theme.MoneyFigureLarge
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.RecurringRuleViewModel
import com.example.financeflow.viewmodel.TransactionViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberRecurringRuleViewModel
import com.example.financeflow.viewmodel.rememberTransactionViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val UPCOMING_HORIZON_DAYS = 7L

@Composable
fun HomeScreen(
    onAddTransaction: (TransactionType?) -> Unit,
    onEditTransaction: (Long) -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel(),
    recurringRuleViewModel: RecurringRuleViewModel = rememberRecurringRuleViewModel()
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

    LaunchedEffect(typeFilter) { recurringRuleViewModel.setTypeFilter(typeFilter) }
    val recurringRules by recurringRuleViewModel.filteredRules.collectAsState()
    val upcomingRules = remember(recurringRules) {
        val today = LocalDate.now()
        val horizon = today.plusDays(UPCOMING_HORIZON_DAYS)
        recurringRules
            .filter { it.active && !it.nextDueDate.isBefore(today) && !it.nextDueDate.isAfter(horizon) }
            .sortedBy { it.nextDueDate }
    }

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = { onAddTransaction(typeFilter) }, contentDescription = stringResource(R.string.home_add_transaction_content_description)) {
                Icon(PhosphorIcons.Regular.Plus, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.nav_home),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.lg)
            )

            TransactionTypeToggle(
                selected = typeFilter,
                onSelect = transactionViewModel::setTypeFilter,
                pill = true
            )

            Spacer(Modifier.height(Spacing.lg))

            Crossfade(targetState = typeFilter, label = "homeTypeFilterContent") { _ ->
                // One LazyColumn for the whole page: the stack is now taller than a phone screen,
                // and the previous nested LazyColumn-inside-Column could not scroll as a unit.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    item(key = "balance") {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentPadding = Spacing.xl
                        ) {
                            Text(
                                text = stringResource(R.string.home_net_this_month),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = currencyFormat.format(summary.income - summary.expense),
                                style = MoneyFigureLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    item(key = "summary") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            SummaryCard(
                                label = stringResource(R.string.toggle_income),
                                amount = "+" + currencyFormat.format(summary.income),
                                amountColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                label = stringResource(R.string.home_expenses_label),
                                amount = "-" + currencyFormat.format(summary.expense),
                                amountColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (upcomingRules.isNotEmpty()) {
                        item(key = "upcomingHeader") {
                            SectionHeader(stringResource(R.string.home_upcoming_title))
                        }
                        items(upcomingRules, key = { "rule-" + it.id }) { rule ->
                            UpcomingRuleRow(rule = rule)
                        }
                    }

                    item(key = "recentHeader") {
                        SectionHeader(stringResource(R.string.home_recent_transactions_title))
                    }

                    if (transactions.isEmpty()) {
                        item(key = "recentEmpty") {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm), contentAlignment = Alignment.Center) {
                                Text(text = stringResource(R.string.transactions_empty), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        items(transactions.take(5), key = { "txn-" + it.id }) { transaction ->
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

                    // Clears the FAB so the last row is never trapped underneath it.
                    item(key = "fabSpacer") { Spacer(Modifier.height(Spacing.xxxl + Spacing.xxl)) }
                }
            }
        }
    }
}

/** Income / Expenses tile — ordinary card fill, label above a single figure. */
@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    amountColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, contentPadding = Spacing.lg) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = amount,
            style = MoneyFigure,
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.sm)
    )
}

@Composable
private fun UpcomingRuleRow(rule: RecurringRule) {
    val currencyFormat = rememberCurrencyFormat(rule.currency)
    val today = LocalDate.now()
    val daysUntilDue = ChronoUnit.DAYS.between(today, rule.nextDueDate)
    val dueLabel = if (daysUntilDue <= 0) {
        stringResource(R.string.home_upcoming_due_today)
    } else {
        stringResource(R.string.home_upcoming_due_in_days, daysUntilDue.toInt())
    }

    GlassRow(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            val sign = if (rule.isIncome) "+" else "-"
            Text(
                text = "$sign${currencyFormat.format(rule.amount)}",
                style = MoneyFigure,
                color = if (rule.isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
