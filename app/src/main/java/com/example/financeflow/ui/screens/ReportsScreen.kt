package com.example.financeflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.components.GlassRow
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.components.periodLabel
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.PeriodTotal
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.CurrencyPreferences
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
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val context = LocalContext.current
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
    val currencyFormat = rememberCurrencyFormat(displayCurrency)
    val locale = currentAppLocale()
    val uncategorized = stringResource(R.string.category_uncategorized)

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

                Spacer(Modifier.height(12.dp))

                TransactionTypeToggle(selected = type, onSelect = reportsViewModel::setTypeFilter)

                Spacer(Modifier.height(20.dp))

                if (period == ReportPeriod.DAY) {
                    DailyReportList(dailyTransactions, categoryNames, uncategorized, currencyFormat, displayCurrency, rates)
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
    currencyFormat: NumberFormat,
    displayCurrency: Currency,
    rates: ExchangeRateCache
) {
    if (transactions.isEmpty()) {
        Text(text = stringResource(R.string.report_no_transactions_today), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val ordered = remember(transactions) { transactions.sortedWith(compareBy({ it.date }, { it.id })) }
    var running = 0.0

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ordered.forEach { transaction ->
            val convertedAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates)
            running += if (transaction.isIncome) convertedAmount else -convertedAmount
            GlassRow(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = categoryNames[transaction.categoryId] ?: uncategorized, style = MaterialTheme.typography.bodyLarge)
                        transaction.note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (transaction.isIncome) "+" else "-"
                        Text(
                            text = "$sign${currencyFormat.format(convertedAmount)}",
                            style = MoneyFigure,
                            color = if (transaction.isIncome) Income else Expense
                        )
                        Text(text = stringResource(R.string.report_running_total, currencyFormat.format(running)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// Bucket labels ride along in the model's own ExtraStore (set in the same runTransaction as the
// series data below) instead of being read from a Composable-scope `labels` list. `runTransaction`
// runs on a background dispatcher, so the axis can still be rendering the *previous* model (e.g.
// Yearly's 12 buckets) for a frame or two after `labels` has already recomposed to the *new,
// shorter* list (e.g. Weekly's 7) — indexing that mismatched list is what produced the empty
// strings Vico rejects. Reading `context.model.extraStore` instead ties each label set to the
// exact model version being drawn, so there's no window where they can disagree.
private val reportBarChartLabelsKey = ExtraStore.Key<List<String>>()

@Composable
private fun ReportBarChart(data: List<PeriodTotal>, period: ReportPeriod, locale: Locale) {
    if (data.isEmpty()) {
        Text(text = stringResource(R.string.report_no_spending_period), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    val expenseValues = remember(data) { data.map { it.expense } }
    val labels = remember(data, period, locale) { data.map { bucketLabel(it.bucket, period, locale) } }

    LaunchedEffect(expenseValues, labels) {
        modelProducer.runTransaction {
            columnSeries { series(expenseValues) }
            extras { it[reportBarChartLabelsKey] = labels }
        }
    }

    val visibleState = remember(data) { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = CartesianValueFormatter { context, value, _ ->
                        resolveBucketLabel(context.model.extraStore.getOrNull(reportBarChartLabelsKey), value)
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}

// Pure and defensive on top of the ExtraStore fix above: even if this is ever asked for an index
// outside the current label set (a measurement pass, a boundary rounding), it must still return a
// non-empty string — Vico throws on "" (see CartesianValueFormatter.formatForAxis). Falls back to
// the nearest real label rather than a placeholder, since every index in range always has one.
internal fun resolveBucketLabel(labels: List<String>?, value: Double): String {
    if (labels.isNullOrEmpty()) return " "
    val index = value.toInt().coerceIn(0, labels.size - 1)
    return labels[index]
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

    val progress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            var cumulativeDegrees = 0f
            val animatedTotalDegrees = 360f * progress.value
            slices.forEach { slice ->
                val fullSweep = (slice.total / total * 360.0).toFloat()
                val drawnSweep = (animatedTotalDegrees - cumulativeDegrees).coerceIn(0f, fullSweep)
                if (drawnSweep > 0f) {
                    drawArc(
                        color = slice.category.color.toCategoryColor(),
                        startAngle = startAngle,
                        sweepAngle = drawnSweep,
                        useCenter = true
                    )
                }
                startAngle += fullSweep
                cumulativeDegrees += fullSweep
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
