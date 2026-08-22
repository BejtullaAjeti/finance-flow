package com.example.financeflow.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.financeflow.R

// ponytail: enum constants are initialized once at class-load, so this stores a @StringRes id
// (not a resolved String) — the label is only ever resolved via stringResource() at the point of
// use, so it stays correct across a runtime language switch instead of being frozen at first load.
enum class FinanceFlowDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Home("home", R.string.nav_home, Icons.Rounded.Home),
    Transactions("transactions", R.string.nav_transactions, Icons.Rounded.ReceiptLong),
    Reports("reports", R.string.nav_reports, Icons.Rounded.BarChart),
    Settings("settings", R.string.nav_settings, Icons.Rounded.Settings)
}

// Budgets and Recurring are no longer bottom-nav destinations (folded into Reports' tab and
// Settings' row respectively) but keep their own routes for the NavHost to register.
const val BUDGETS_ROUTE = "budgets"
const val RECURRING_ROUTE = "recurring"

private const val ADD_EDIT_TRANSACTION_BASE_ROUTE = "transaction"
const val ADD_EDIT_TRANSACTION_ROUTE = "$ADD_EDIT_TRANSACTION_BASE_ROUTE?transactionId={transactionId}"

fun editTransactionRoute(transactionId: Long) = "$ADD_EDIT_TRANSACTION_BASE_ROUTE?transactionId=$transactionId"

const val CATEGORIES_ROUTE = "categories"

private const val ADD_EDIT_RECURRING_RULE_BASE_ROUTE = "recurring_rule"
const val ADD_EDIT_RECURRING_RULE_ROUTE = "$ADD_EDIT_RECURRING_RULE_BASE_ROUTE?ruleId={ruleId}"

fun editRecurringRuleRoute(ruleId: Long) = "$ADD_EDIT_RECURRING_RULE_BASE_ROUTE?ruleId=$ruleId"
