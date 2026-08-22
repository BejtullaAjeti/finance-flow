package com.example.financeflow.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.ui.theme.MoneyFigure
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.RecurringRuleViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberRecurringRuleViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Composable
fun RecurringRulesScreen(
    onAddRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    recurringRuleViewModel: RecurringRuleViewModel = rememberRecurringRuleViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val rules by recurringRuleViewModel.filteredRules.collectAsState()
    val typeFilter by recurringRuleViewModel.currentTypeFilter.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val currencyFormat = rememberCurrencyFormat()
    val dateFormat = rememberDateFormat("MMM d, yyyy")
    val uncategorized = stringResource(R.string.category_uncategorized)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.recurring_add_content_description))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            TransactionTypeToggle(
                selected = typeFilter,
                onSelect = recurringRuleViewModel::setTypeFilter,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.recurring_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rules, key = { it.id }) { rule ->
                        RecurringRuleRow(
                            rule = rule,
                            categoryName = categoryNames[rule.categoryId] ?: uncategorized,
                            currencyFormat = currencyFormat,
                            dateFormat = dateFormat,
                            onClick = { onEditRule(rule.id) },
                            onToggleActive = { active ->
                                recurringRuleViewModel.updateRule(rule.copy(active = active))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringRuleRow(
    rule: RecurringRule,
    categoryName: String,
    currencyFormat: NumberFormat,
    dateFormat: DateTimeFormatter,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = rule.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.recurring_row_summary, categoryName, frequencyLabel(rule), rule.nextDueDate.format(dateFormat)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sign = if (rule.isIncome) "+" else "-"
            Text(
                text = "$sign${currencyFormat.format(rule.amount)}",
                style = MoneyFigure,
                color = if (rule.isIncome) Income else Expense
            )
            Switch(checked = rule.active, onCheckedChange = onToggleActive)
        }
    }
}

@Composable
private fun frequencyLabel(rule: RecurringRule): String = when (rule.frequency) {
    Frequency.WEEKLY -> stringResource(R.string.label_weekly)
    Frequency.MONTHLY -> stringResource(R.string.label_monthly)
    Frequency.CUSTOM -> stringResource(R.string.frequency_every_n_days, rule.customIntervalDays ?: 1)
}
