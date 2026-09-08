package com.example.financeflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.LocalePreferences
import com.example.financeflow.recurring.RecurringRuleProcessor
import com.example.financeflow.ui.navigation.FinanceFlowApp
import com.example.financeflow.ui.theme.FinanceFlowTheme
import com.example.financeflow.ui.theme.ThemeMode
import com.example.financeflow.ui.theme.ThemePreferences
import com.example.financeflow.work.RecurringRuleWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalePreferences.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        RecurringRuleWorker.schedule(applicationContext)
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            CategoryRepository(db.categoryDao()).seedDefaultsIfEmpty(defaultCategories())
            RecurringRuleProcessor(db).generateDueTransactions()
            ExchangeRateRepository(db.exchangeRateDao(), applicationContext).refreshIfStale()
        }

        setContent {
            val themeMode by ThemePreferences.flow(applicationContext).collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FinanceFlowTheme(darkTheme = darkTheme) {
                FinanceFlowApp()
            }
        }
    }

    // `name` is only a fallback snapshot (Room requires a non-null value) — every one of these is
    // resolved through its `nameKey` at display time (Category.displayName()) so it re-translates
    // when the language toggle changes, instead of staying frozen in whatever locale was active
    // on first launch.
    private fun defaultCategories(): List<Category> = listOf(
        Category(name = getString(R.string.category_default_food), nameKey = "food", type = CategoryType.PERSONAL, icon = "food"),
        Category(name = getString(R.string.category_default_transport), nameKey = "transport", type = CategoryType.PERSONAL, icon = "car"),
        Category(name = getString(R.string.category_default_bills), nameKey = "bills", type = CategoryType.PERSONAL, icon = "bill"),
        Category(name = getString(R.string.category_default_salary), nameKey = "salary", type = CategoryType.PERSONAL, icon = "income"),
        Category(name = getString(R.string.category_default_business_income), nameKey = "business_income", type = CategoryType.BUSINESS, icon = "income"),
        Category(name = getString(R.string.category_default_business_expenses), nameKey = "business_expenses", type = CategoryType.BUSINESS, icon = "work"),
        Category(name = getString(R.string.category_default_others), nameKey = "others", type = CategoryType.PERSONAL),
        Category(name = getString(R.string.category_default_others), nameKey = "others", type = CategoryType.BUSINESS)
    )
}
