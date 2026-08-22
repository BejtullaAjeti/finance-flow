package com.example.financeflow.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.example.financeflow.data.Currency
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The locale this Context is actually configured for. [LocalePreferences.wrap] also mutates the
 * global [Locale.getDefault], and `NumberFormat`/`DateTimeFormatter` fall back to that when no
 * locale is passed — but reading it from [LocalConfiguration] here is the robust, recomposition-
 * aware source of truth, not a mutable JVM global that something else could reset mid-process.
 */
@Composable
fun currentAppLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun rememberCurrencyFormat(currency: Currency): NumberFormat {
    val locale = currentAppLocale()
    return remember(locale, currency) {
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = java.util.Currency.getInstance(currency.name)
        }
    }
}

@Composable
fun rememberDateFormat(pattern: String): DateTimeFormatter {
    val locale = currentAppLocale()
    return remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
}
