package com.example.financeflow.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.CategoryIcons
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.viewmodel.BudgetViewModel
import com.example.financeflow.viewmodel.CategoryBudget
import com.example.financeflow.viewmodel.rememberBudgetViewModel
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    budgetViewModel: BudgetViewModel = rememberBudgetViewModel()
) {
    val budgets by budgetViewModel.budgets.collectAsState()
    val currencyFormat = rememberCurrencyFormat()

    val typeFilter by budgetViewModel.currentTypeFilter.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TransactionTypeToggle(
            selected = typeFilter,
            onSelect = budgetViewModel::setTypeFilter
        )

        Spacer(Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.budgets_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets, key = { it.category.id }) { budget ->
                    BudgetCard(budget = budget, currencyFormat = currencyFormat)
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: CategoryBudget, currencyFormat: NumberFormat) {
    val fraction = if (budget.limit > 0) (budget.spent / budget.limit).toFloat() else 0f
    val isOverBudget = budget.spent > budget.limit
    val statusColor = if (isOverBudget) Expense else Income
    val swatch = budget.category.color.toCategoryColor()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                CategoryIcons.resolve(budget.category.icon),
                contentDescription = null,
                tint = swatch
            )
            Spacer(Modifier.width(8.dp))
            Text(text = budget.category.name, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = statusColor,
            trackColor = statusColor.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.budget_spent_of_limit, currencyFormat.format(budget.spent), currencyFormat.format(budget.limit)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(fraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )
        }

        if (isOverBudget) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Expense)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.budget_over_amount, currencyFormat.format(budget.spent - budget.limit)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Expense
                )
            }
        }
    }
}
