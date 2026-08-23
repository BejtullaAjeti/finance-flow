package com.example.financeflow.locale

import android.content.Context
import androidx.core.content.edit
import com.example.financeflow.data.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_DISPLAY_CURRENCY = "display_currency"

/**
 * Unlike [LocalePreferences] (which needs an Activity recreate to re-resolve string resources),
 * currency display is just recomputed formatting — so this exposes a live StateFlow instead,
 * letting Home/Budgets/Reports react immediately when the user changes it in Settings.
 */
object CurrencyPreferences {
    private var state: MutableStateFlow<Currency>? = null

    fun flow(context: Context): StateFlow<Currency> =
        state ?: MutableStateFlow(read(context)).also { state = it }

    fun set(context: Context, currency: Currency) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_DISPLAY_CURRENCY, currency.name)
        }
        val flow = state ?: MutableStateFlow(currency).also { state = it }
        flow.value = currency
    }

    private fun read(context: Context): Currency =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_CURRENCY, null)
            ?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
            ?: Currency.EUR
}
