package com.example.financeflow.ui.navigation

import androidx.annotation.StringRes
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.ChartBar
import com.adamglin.phosphoricons.fill.Gear
import com.adamglin.phosphoricons.fill.House
import com.adamglin.phosphoricons.fill.Receipt
import com.adamglin.phosphoricons.regular.ChartBar
import com.adamglin.phosphoricons.regular.Gear
import com.adamglin.phosphoricons.regular.House
import com.adamglin.phosphoricons.regular.Receipt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.financeflow.R
import com.example.financeflow.data.TransactionType

// ponytail: enum constants are initialized once at class-load, so this stores a @StringRes id
// (not a resolved String) — the label is only ever resolved via stringResource() at the point of
// use, so it stays correct across a runtime language switch instead of being frozen at first load.
enum class FinanceFlowDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val filledIcon: ImageVector
) {
    Home("home", R.string.nav_home, PhosphorIcons.Regular.House, PhosphorIcons.Fill.House),
    Transactions("transactions", R.string.nav_transactions, PhosphorIcons.Regular.Receipt, PhosphorIcons.Fill.Receipt),
    Reports("reports", R.string.nav_reports, PhosphorIcons.Regular.ChartBar, PhosphorIcons.Fill.ChartBar),
    Settings("settings", R.string.nav_settings, PhosphorIcons.Regular.Gear, PhosphorIcons.Fill.Gear)
}

// Budgets and Recurring are no longer bottom-nav destinations (folded into Reports' tab and
// Settings' row respectively) but keep their own routes for the NavHost to register.
const val BUDGETS_ROUTE = "budgets"
const val RECURRING_ROUTE = "recurring"

private const val ADD_EDIT_TRANSACTION_BASE_ROUTE = "transaction"
const val ADD_EDIT_TRANSACTION_ROUTE = "$ADD_EDIT_TRANSACTION_BASE_ROUTE?transactionId={transactionId}&suggestedType={suggestedType}"

fun editTransactionRoute(transactionId: Long) = "$ADD_EDIT_TRANSACTION_BASE_ROUTE?transactionId=$transactionId"

// Only Personal/Business narrows the add-transaction default — a Combined/neutral context passes
// null and leaves the last-used type (LastUsedTypePreferences) as the fallback.
fun addTransactionRoute(suggestedType: TransactionType? = null): String =
    if (suggestedType != null) "$ADD_EDIT_TRANSACTION_BASE_ROUTE?suggestedType=${suggestedType.name}"
    else ADD_EDIT_TRANSACTION_BASE_ROUTE

const val CATEGORIES_ROUTE = "categories"

private const val ADD_EDIT_RECURRING_RULE_BASE_ROUTE = "recurring_rule"
const val ADD_EDIT_RECURRING_RULE_ROUTE = "$ADD_EDIT_RECURRING_RULE_BASE_ROUTE?ruleId={ruleId}"

fun editRecurringRuleRoute(ruleId: Long) = "$ADD_EDIT_RECURRING_RULE_BASE_ROUTE?ruleId=$ruleId"
