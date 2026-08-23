package com.example.financeflow.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.LocalSnackbarController
import com.example.financeflow.ui.components.SnackbarController
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.screens.AddEditRecurringRuleScreen
import com.example.financeflow.ui.screens.AddEditTransactionScreen
import com.example.financeflow.ui.screens.BudgetsScreen
import com.example.financeflow.ui.screens.CategoriesScreen
import com.example.financeflow.ui.screens.HomeScreen
import com.example.financeflow.ui.screens.RecurringRulesScreen
import com.example.financeflow.ui.screens.ReportsScreen
import com.example.financeflow.ui.screens.SettingsScreen
import com.example.financeflow.ui.screens.TransactionsScreen

@Composable
fun FinanceFlowApp(navController: NavHostController = rememberNavController()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarController = remember { SnackbarController(snackbarHostState, coroutineScope) }

    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
    Scaffold(
        bottomBar = { FinanceFlowBottomBar(navController) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                GlassCard(tier = GlassTier.Overlay, contentPadding = 16.dp) {
                    Text(data.visuals.message)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FinanceFlowDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(FinanceFlowDestination.Home.route) {
                HomeScreen(
                    onAddTransaction = { navController.navigate("transaction") },
                    onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) }
                )
            }
            composable(FinanceFlowDestination.Transactions.route) {
                TransactionsScreen(
                    onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) },
                    onAddTransaction = { navController.navigate("transaction") }
                )
            }
            composable(BUDGETS_ROUTE) { BudgetsScreen() }
            composable(FinanceFlowDestination.Reports.route) { ReportsScreen() }
            composable(RECURRING_ROUTE) {
                RecurringRulesScreen(
                    onAddRule = { navController.navigate("recurring_rule") },
                    onEditRule = { id -> navController.navigate(editRecurringRuleRoute(id)) }
                )
            }
            composable(FinanceFlowDestination.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = { navController.navigate(CATEGORIES_ROUTE) },
                    onNavigateToRecurring = { navController.navigate(RECURRING_ROUTE) }
                )
            }
            composable(CATEGORIES_ROUTE) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = ADD_EDIT_TRANSACTION_ROUTE,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId")?.takeIf { it != -1L }
                AddEditTransactionScreen(
                    transactionId = transactionId,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(
                route = ADD_EDIT_RECURRING_RULE_ROUTE,
                arguments = listOf(navArgument("ruleId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getLong("ruleId")?.takeIf { it != -1L }
                AddEditRecurringRuleScreen(
                    ruleId = ruleId,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

// Glass-styled per CLAUDE.md's Design & Styling section, which names the bottom nav bar
// alongside the dashboard summary and budget cards as an intended frosted-glass surface.
@Composable
private fun FinanceFlowBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        contentPadding = 0.dp
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            FinanceFlowDestination.entries.forEach { destination ->
                val label = stringResource(destination.labelRes)
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destination.icon, contentDescription = label) },
                    label = { Text(label) }
                )
            }
        }
    }
}
