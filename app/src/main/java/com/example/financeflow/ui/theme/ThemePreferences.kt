package com.example.financeflow.ui.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_THEME_MODE = "theme_mode"

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Same live-StateFlow shape as CurrencyPreferences — a theme switch is just a recomposition,
// no Activity recreate needed, so every screen picks it up immediately.
object ThemePreferences {
    private var state: MutableStateFlow<ThemeMode>? = null

    fun flow(context: Context): StateFlow<ThemeMode> =
        state ?: MutableStateFlow(read(context)).also { state = it }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_THEME_MODE, mode.name)
        }
        val flow = state ?: MutableStateFlow(mode).also { state = it }
        flow.value = mode
    }

    private fun read(context: Context): ThemeMode =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
}
