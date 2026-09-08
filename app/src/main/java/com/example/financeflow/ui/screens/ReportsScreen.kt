package com.example.financeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.components.DateField
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.components.ListRow
import com.example.financeflow.ui.components.periodLabel
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.CategoryIcons
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.components.displayName
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.MoneyFigure
import com.example.financeflow.ui.theme.MoneyFigureLarge
import com.example.financeflow.ui.theme.extendedColors
import com.example.financeflow.viewmodel.CategorySlice
import com.example.financeflow.viewmodel.ReportsViewModel
import com.example.financeflow.viewmodel.rememberReportsViewModel
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel = rememberReportsViewModel()
) {
    val period by reportsViewModel.selectedPeriod.collectAsState()
    val type by reportsViewModel.selectedType.collectAsState()
    val dailyDate by reportsViewModel.selectedDailyDate.collectAsState()
    val categoryBreakdown by reportsViewModel.categoryBreakdown.collectAsState()
    val context = LocalContext.current
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    val currencyFormat = rememberCurrencyFormat(displayCurrency)

    var selectedTab by remember { mutableIntStateOf(0) }
    val overviewLabel = stringResource(R.string.report_tab_overview)
    val budgetsLabel = stringResource(R.string.report_tab_budgets)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GlassSegmentedControl(
            options = listOf(0, 1),
            selected = selectedTab,
            onSelect = { selectedTab = it },
            label = { if (it == 0) overviewLabel else budgetsLabel }
        )

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val periodLabels = ReportPeriod.entries.associateWith { periodLabel(it) }
                GlassSegmentedControl(
                    options = ReportPeriod.entries,
                    selected = period,
                    onSelect = { reportsViewModel.setPeriod(it) },
                    label = { periodLabels[it] ?: "" }
                )

                if (period == ReportPeriod.DAY) {
                    Spacer(Modifier.height(12.dp))
                    DateField(
                        label = stringResource(R.string.field_date_label),
                        date = dailyDate,
                        onDateChange = { reportsViewModel.setDailyDate(it) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                TransactionTypeToggle(selected = type, onSelect = reportsViewModel::setTypeFilter)

                Spacer(Modifier.height(20.dp))

                val totalIncome = remember(categoryBreakdown) { categoryBreakdown.sumOf { it.income } }
                val totalExpense = remember(categoryBreakdown) { categoryBreakdown.sumOf { it.expense } }
                ReportTotals(totalIncome, totalExpense, currencyFormat)

                Spacer(Modifier.height(24.dp))

                val incomeSlices = remember(categoryBreakdown) {
                    categoryBreakdown.filter { it.income > 0 }.sortedByDescending { it.income }
                }
                val expenseSlices = remember(categoryBreakdown) {
                    categoryBreakdown.filter { it.expense > 0 }.sortedByDescending { it.expense }
                }

                ReportCategorySection(
                    title = stringResource(R.string.toggle_income),
                    slices = incomeSlices,
                    total = totalIncome,
                    amountOf = { it.income },
                    emptyText = stringResource(R.string.report_no_income_period),
                    currencyFormat = currencyFormat
                )

                Spacer(Modifier.height(24.dp))

                ReportCategorySection(
                    title = stringResource(R.string.home_expenses_label),
                    slices = expenseSlices,
                    total = totalExpense,
                    amountOf = { it.expense },
                    emptyText = stringResource(R.string.report_no_spending_period),
                    currencyFormat = currencyFormat
                )
            }
        } else {
            BudgetsScreen()
        }
    }
}

@Composable
private fun ReportTotals(income: Double, expense: Double, currencyFormat: NumberFormat) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = stringResource(R.string.toggle_income), style = MaterialTheme.typography.bodyMedium)
                Text(text = currencyFormat.format(income), style = MoneyFigureLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = stringResource(R.string.home_expenses_label), style = MaterialTheme.typography.bodyMedium)
                Text(text = currencyFormat.format(expense), style = MoneyFigureLarge, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

// A genuine stacked bar (no axes, no animation) — one segment per category, sized by its share of
// `total` and colored with its assigned color — plus a dot+label legend beneath it, and (separate
// from and below that) the plain amount+percentage list. Reports' numbers-first replacement for
// the old Vico bar chart and Canvas pie chart.
@Composable
private fun ReportCategorySection(
    title: String,
    slices: List<CategorySlice>,
    total: Double,
    amountOf: (CategorySlice) -> Double,
    emptyText: String,
    currencyFormat: NumberFormat
) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))

    if (slices.isEmpty() || total <= 0.0) {
        Text(text = emptyText, style = MaterialTheme.typography.bodyMedium)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(Radius.small))
    ) {
        slices.forEach { slice ->
            Box(
                modifier = Modifier
                    .weight(amountOf(slice).toFloat().coerceAtLeast(0.0001f))
                    .fillMaxHeight()
                    .background(slice.category.color.toCategoryColor())
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(slice.category.color.toCategoryColor(), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = slice.category.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        slices.forEach { slice ->
            val amount = amountOf(slice)
            val percent = (amount / total * 100).let { if (it >= 10) it.toInt().toString() else String.format("%.1f", it) }
            CategoryAmountRow(slice.category, amount, percent, currencyFormat)
        }
    }
}

@Composable
private fun CategoryAmountRow(category: Category, amount: Double, percent: String, currencyFormat: NumberFormat) {
    ListRow(
        icon = CategoryIcons.resolve(category.icon),
        swatchColor = category.color.toCategoryColor(),
        title = category.displayName(),
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = currencyFormat.format(amount), style = MoneyFigure)
                Text(
                    text = stringResource(R.string.report_percent_of_total, percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
