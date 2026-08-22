package com.example.financeflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.Currency
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.CategoryIcons
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassDialog
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.GlassFilterChip
import com.example.financeflow.ui.components.InlineHint
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.viewmodel.BudgetViewModel
import com.example.financeflow.viewmodel.CategoryBudget
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.rememberBudgetViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import java.text.NumberFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    budgetViewModel: BudgetViewModel = rememberBudgetViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val budgets by budgetViewModel.budgets.collectAsState()
    val displayCurrency by CurrencyPreferences.flow(LocalContext.current).collectAsState()
    val currencyFormat = rememberCurrencyFormat(displayCurrency)

    val typeFilter by budgetViewModel.currentTypeFilter.collectAsState()

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var pickingCategory by remember { mutableStateOf(false) }
    var showBudgetHint by remember { mutableStateOf(true) }

    val unbudgeted = remember(budgets) { budgets.filter { it.limit == null }.map { it.category } }
    val noneHaveBudgets = budgets.isNotEmpty() && unbudgeted.size == budgets.size

    Scaffold(
        floatingActionButton = {
            if (unbudgeted.isNotEmpty()) {
                GlassFab(onClick = { pickingCategory = true }, contentDescription = stringResource(R.string.budget_add_content_description)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            TransactionTypeToggle(
                selected = typeFilter,
                onSelect = budgetViewModel::setTypeFilter
            )

            Spacer(Modifier.height(16.dp))

            if (noneHaveBudgets && showBudgetHint) {
                InlineHint(
                    text = stringResource(R.string.hint_budgets_empty),
                    onDismiss = { showBudgetHint = false }
                )
                Spacer(Modifier.height(8.dp))
            }

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
                        BudgetCard(
                            budget = budget,
                            currencyFormat = currencyFormat,
                            onClick = { editingCategory = budget.category }
                        )
                    }
                }
            }
        }
    }

    editingCategory?.let { category ->
        SetBudgetDialog(
            fixedCategory = category,
            pickableCategories = emptyList(),
            onDismiss = { editingCategory = null },
            onSave = { selected, amount, currency ->
                categoryViewModel.updateCategory(selected.copy(budgetLimit = amount, budgetLimitCurrency = currency))
                editingCategory = null
            }
        )
    }

    if (pickingCategory) {
        SetBudgetDialog(
            fixedCategory = null,
            pickableCategories = unbudgeted,
            onDismiss = { pickingCategory = false },
            onSave = { selected, amount, currency ->
                categoryViewModel.updateCategory(selected.copy(budgetLimit = amount, budgetLimitCurrency = currency))
                pickingCategory = false
            }
        )
    }
}

@Composable
private fun BudgetCard(budget: CategoryBudget, currencyFormat: NumberFormat, onClick: () -> Unit) {
    val limit = budget.limit
    val swatch = budget.category.color.toCategoryColor()

    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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

        if (limit == null) {
            Text(
                text = stringResource(R.string.budget_spent_no_limit, currencyFormat.format(budget.spent)),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.budget_not_set),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            val fraction = if (limit > 0) (budget.spent / limit).toFloat() else 0f
            val isOverBudget = budget.spent > limit
            val statusColor = if (isOverBudget) Expense else Income

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
                    text = stringResource(R.string.budget_spent_of_limit, currencyFormat.format(budget.spent), currencyFormat.format(limit)),
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
                        text = stringResource(R.string.budget_over_amount, currencyFormat.format(budget.spent - limit)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Expense
                    )
                }
            }
        }
    }
}

// Shared by both entry points: tapping an existing card (fixedCategory set, pickableCategories
// empty) and the "+ Add Budget" FAB (fixedCategory null, pickableCategories the unbudgeted list).
@Composable
private fun SetBudgetDialog(
    fixedCategory: Category?,
    pickableCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category, Double, Currency) -> Unit
) {
    var selected by remember { mutableStateOf(fixedCategory) }
    var amountText by remember { mutableStateOf(fixedCategory?.budgetLimit?.toString().orEmpty()) }
    var currency by remember { mutableStateOf(fixedCategory?.budgetLimitCurrency ?: Currency.MKD) }

    val canSave = selected != null && amountText.toDoubleOrNull()?.let { it > 0 } == true

    GlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(selected?.name ?: stringResource(R.string.budget_pick_category_title)) },
        text = {
            Column {
                if (fixedCategory == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pickableCategories.forEach { category ->
                            GlassFilterChip(
                                selected = selected?.id == category.id,
                                onClick = { selected = category },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = input
                        }
                    },
                    label = { Text(stringResource(R.string.budget_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Currency.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            icon = {},
                            selected = currency == option,
                            onClick = { currency = option },
                            shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
                        ) { Text(option.name) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val category = selected ?: return@TextButton
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    onSave(category, amount, currency)
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
