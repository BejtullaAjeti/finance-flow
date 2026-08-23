package com.example.financeflow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.data.ReportPeriod

// ponytail: resolved via a @Composable function (not a top-level val map) so the labels
// re-resolve on recomposition instead of being frozen in whatever locale was active at class-load.
// Shared by Reports (the period picker) and Budgets (the per-budget period selector/display).
@Composable
fun periodLabel(period: ReportPeriod): String = when (period) {
    ReportPeriod.DAY -> stringResource(R.string.label_daily)
    ReportPeriod.WEEK -> stringResource(R.string.label_weekly)
    ReportPeriod.MONTH -> stringResource(R.string.label_monthly)
    ReportPeriod.YEAR -> stringResource(R.string.label_yearly)
}
