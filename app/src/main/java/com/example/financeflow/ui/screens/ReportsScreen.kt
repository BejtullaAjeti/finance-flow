package com.example.financeflow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.PeriodTotal
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.Transaction
import com.example.financeflow.locale.currentAppLocale
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.ui.theme.MoneyFigure
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.CategorySlice
import com.example.financeflow.viewmodel.ReportsViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberReportsViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ponytail: resolved via a @Composable function (not a top-level val map) so the labels
// re-resolve on recomposition instead of being frozen in whatever locale was active at class-load.
@Composable
private fun periodLabel(period: ReportPeriod): String = when (period) {
    ReportPeriod.DAY -> stringResource(R.string.label_daily)
    ReportPeriod.WEEK -> stringResource(R.string.label_weekly)
    ReportPeriod.MONTH -> stringResource(R.string.label_monthly)
    ReportPeriod.YEAR -> stringResource(R.string.label_yearly)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reportsViewModel: ReportsViewModel = rememberReportsViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val period by reportsViewModel.selectedPeriod.collectAsState()
    val type by reportsViewModel.selectedType.collectAsState()
    val dailyTransactions by reportsViewModel.dailyTransactions.collectAsState()
    val barChartData by reportsViewModel.barChartData.collectAsState()
    val categoryBreakdown by reportsViewModel.categoryBreakdown.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val currencyFormat = rememberCurrencyFormat()
    val locale = currentAppLocale()
    val uncategorized = stringResource(R.string.category_uncategorized)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf(stringResource(R.string.report_tab_overview), stringResource(R.string.report_tab_budgets))

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabLabels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index, tabLabels.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReportPeriod.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = period == option,
                            onClick = { reportsViewModel.setPeriod(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size)
                        ) {
                            Text(periodLabel(option))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                TransactionTypeToggle(selected = type, onSelect = reportsViewModel::setTypeFilter)

                Spacer(Modifier.height(20.dp))

                if (period == ReportPeriod.DAY) {
                    DailyReportList(dailyTransactions, categoryNames, uncategorized, currencyFormat)
                } else {
                    ReportBarChart(barChartData, period, locale)
                }

                Spacer(Modifier.height(24.dp))

                Text(text = stringResource(R.string.report_by_category_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                CategoryPieChart(categoryBreakdown, currencyFormat)
            }
        } else {
            BudgetsScreen()
        }
    }
}

@Composable
private fun DailyReportList(
    transactions: List<Transaction>,
    categoryNames: Map<Long, String>,
    uncategorized: String,
    currencyFormat: NumberFormat
) {
    if (transactions.isEmpty()) {
        Text(text = stringResource(R.string.report_no_transactions_today), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val ordered = remember(transactions) { transactions.sortedWith(compareBy({ it.date }, { it.id })) }
    var running = 0.0

    Column {
        ordered.forEach { transaction ->
            running += if (transaction.isIncome) transaction.amount else -transaction.amount
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = categoryNames[transaction.categoryId] ?: uncategorized, style = MaterialTheme.typography.bodyLarge)
                    transaction.note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val sign = if (transaction.isIncome) "+" else "-"
                    Text(
                        text = "$sign${currencyFormat.format(transaction.amount)}",
                        style = MoneyFigure,
                        color = if (transaction.isIncome) Income else Expense
                    )
                    Text(text = stringResource(R.string.report_running_total, currencyFormat.format(running)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ReportBarChart(data: List<PeriodTotal>, period: ReportPeriod, locale: Locale) {
    if (data.isEmpty()) {
        Text(text = stringResource(R.string.report_no_spending_period), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val expenseValues = remember(data) { data.map { it.expense } }
    val labels = remember(data, period, locale) { data.map { bucketLabel(it.bucket, period, locale) } }

    LaunchedEffect(expenseValues) {
        modelProducer.runTransaction {
            columnSeries { series(expenseValues) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.toInt()).orEmpty() }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

private fun bucketLabel(bucket: String, period: ReportPeriod, locale: Locale): String = when (period) {
    ReportPeriod.WEEK -> LocalDate.parse(bucket).format(DateTimeFormatter.ofPattern("EEE", locale))
    ReportPeriod.MONTH -> LocalDate.parse(bucket).dayOfMonth.toString()
    ReportPeriod.YEAR -> YearMonth.parse(bucket).format(DateTimeFormatter.ofPattern("MMM", locale))
    ReportPeriod.DAY -> bucket
}

// ponytail: Vico (2.1.3) only ships Cartesian layers (column/line/candlestick) — no pie/donut —
// so the category slice is a plain Canvas arc chart instead of a Vico component.
@Composable
private fun CategoryPieChart(slices: List<CategorySlice>, currencyFormat: NumberFormat) {
    val total = slices.sumOf { it.total }
    if (total <= 0.0) {
        Text(text = stringResource(R.string.report_no_spending_period), style = MaterialTheme.typography.bodyMedium)
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.total / total * 360.0).toFloat()
                drawArc(
                    color = slice.category.color.toCategoryColor(),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(slice.category.color.toCategoryColor(), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${slice.category.name} · ${currencyFormat.format(slice.total)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
