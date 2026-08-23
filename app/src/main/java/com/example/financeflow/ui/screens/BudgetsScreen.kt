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
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.WarningCircle
import com.adamglin.phosphoricons.regular.Plus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.AddCategoryButton
import com.example.financeflow.ui.components.CategoryIcons
import com.example.financeflow.ui.components.CancelButton
import com.example.financeflow.ui.components.ConfirmButton
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassDialog
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.GlassFilterChip
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.components.GlassTextField
import com.example.financeflow.ui.components.InlineHint
import com.example.financeflow.ui.components.QuickAddCategoryDialog
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.components.TypeIndicatorIcon
import com.example.financeflow.ui.components.indicatorIcon
import com.example.financeflow.ui.components.periodLabel
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.extendedColors
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
                    Icon(PhosphorIcons.Regular.Plus, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TransactionTypeToggle(
                    selected = typeFilter,
                    onSelect = budgetViewModel::setTypeFilter
                )
            }

            if (noneHaveBudgets && showBudgetHint) {
                item {
                    InlineHint(
                        text = stringResource(R.string.hint_budgets_empty),
                        onDismiss = { showBudgetHint = false }
                    )
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.budgets_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(budgets, key = { it.category.id }) { budget ->
                    BudgetCard(
                        budget = budget,
                        currencyFormat = currencyFormat,
                        onClick = { editingCategory = budget.category },
                        isCombinedView = typeFilter == null
                    )
                }
            }
        }
    }

    val defaultCategoryType = typeFilter?.let(::categoryTypeFor) ?: CategoryType.BOTH

    editingCategory?.let { category ->
        SetBudgetDialog(
            fixedCategory = category,
            pickableCategories = emptyList(),
            defaultCategoryType = defaultCategoryType,
            categoryViewModel = categoryViewModel,
            onDismiss = { editingCategory = null },
            onSave = { selected, amount, currency, period ->
                categoryViewModel.updateCategory(selected.copy(budgetLimit = amount, budgetLimitCurrency = currency, budgetPeriod = period))
                editingCategory = null
            }
        )
    }

    if (pickingCategory) {
        SetBudgetDialog(
            fixedCategory = null,
            pickableCategories = unbudgeted,
            defaultCategoryType = defaultCategoryType,
            categoryViewModel = categoryViewModel,
            onDismiss = { pickingCategory = false },
            onSave = { selected, amount, currency, period ->
                categoryViewModel.updateCategory(selected.copy(budgetLimit = amount, budgetLimitCurrency = currency, budgetPeriod = period))
                pickingCategory = false
            }
        )
    }
}

@Composable
private fun BudgetCard(budget: CategoryBudget, currencyFormat: NumberFormat, onClick: () -> Unit, isCombinedView: Boolean) {
    val limit = budget.limit
    val swatch = budget.category.color.toCategoryColor()

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        containerColor = MaterialTheme.extendedColors.accent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                CategoryIcons.resolve(budget.category.icon),
                contentDescription = null,
                tint = swatch
            )
            Spacer(Modifier.width(8.dp))
            Text(text = budget.category.name, style = MaterialTheme.typography.titleLarge)
            val icon = if (isCombinedView) budget.category.type.indicatorIcon() else null
            if (icon != null) {
                Spacer(Modifier.width(6.dp))
                TypeIndicatorIcon(icon)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (limit == null) {
            Text(
                text = stringResource(R.string.budget_spent_no_limit, currencyFormat.format(budget.spent)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.budget_not_set),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            val fraction = if (limit > 0) (budget.spent / limit).toFloat() else 0f
            val isOverBudget = budget.spent > limit
            val statusColor = when {
                fraction >= 1f -> MaterialTheme.colorScheme.tertiary
                fraction >= 0.8f -> MaterialTheme.extendedColors.warning
                else -> MaterialTheme.colorScheme.secondary
            }
            val statusContainerColor = when {
                fraction >= 1f -> MaterialTheme.colorScheme.tertiaryContainer
                fraction >= 0.8f -> MaterialTheme.extendedColors.warningContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }

            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = statusColor,
                trackColor = statusContainerColor
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.budget_spent_of_limit, currencyFormat.format(budget.spent), currencyFormat.format(limit)) +
                        " · " + periodLabel(budget.period),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Icon(PhosphorIcons.Fill.WarningCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.budget_over_amount, currencyFormat.format(budget.spent - limit)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
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
    defaultCategoryType: CategoryType,
    categoryViewModel: CategoryViewModel,
    onDismiss: () -> Unit,
    onSave: (Category, Double, Currency, ReportPeriod) -> Unit
) {
    var selected by remember { mutableStateOf(fixedCategory) }
    var amountText by remember { mutableStateOf(fixedCategory?.budgetLimit?.toString().orEmpty()) }
    var currency by remember { mutableStateOf(fixedCategory?.budgetLimitCurrency ?: Currency.EUR) }
    var period by remember { mutableStateOf(fixedCategory?.budgetPeriod ?: ReportPeriod.MONTH) }
    var showQuickAddCategory by remember { mutableStateOf(false) }

    val canSave = selected != null && amountText.toDoubleOrNull()?.let { it > 0 } == true
    val periodLabels = ReportPeriod.entries.associateWith { periodLabel(it) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(selected?.name ?: stringResource(R.string.budget_pick_category_title)) },
        text = {
            Column {
                if (fixedCategory == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pickableCategories.forEach { category ->
                            GlassFilterChip(
                                selected = selected?.id == category.id,
                                onClick = { selected = category },
                                label = { Text(category.name) }
                            )
                        }
                        AddCategoryButton(onClick = { showQuickAddCategory = true })
                    }
                    Spacer(Modifier.height(16.dp))
                }

                GlassTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = input
                        }
                    },
                    label = stringResource(R.string.budget_amount_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                GlassSegmentedControl(
                    options = Currency.entries,
                    selected = currency,
                    onSelect = { currency = it },
                    label = { it.name }
                )

                Spacer(Modifier.height(8.dp))

                GlassSegmentedControl(
                    options = ReportPeriod.entries,
                    selected = period,
                    onSelect = { period = it },
                    label = { periodLabels[it] ?: "" }
                )
            }
        },
        confirmButton = {
            ConfirmButton(
                enabled = canSave,
                onClick = {
                    val category = selected ?: return@ConfirmButton
                    val amount = amountText.toDoubleOrNull() ?: return@ConfirmButton
                    onSave(category, amount, currency, period)
                }
            )
        },
        dismissButton = {
            CancelButton(onClick = onDismiss)
        }
    )

    if (showQuickAddCategory) {
        QuickAddCategoryDialog(
            defaultType = defaultCategoryType,
            onDismiss = { showQuickAddCategory = false },
            onSave = { category ->
                categoryViewModel.addCategory(category) { id ->
                    selected = category.copy(id = id)
                }
                showQuickAddCategory = false
            }
        )
    }
}
