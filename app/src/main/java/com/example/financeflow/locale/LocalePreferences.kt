package com.example.financeflow.locale

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

private const val PREFS_NAME = "settings"
private const val KEY_LANGUAGE = "language"

/**
 * Forces the app into "sq" or "en" regardless of the device's system locale, per CLAUDE.md's
 * localization notes. Null means "no override" — Android's normal resource resolution applies
 * (values-en/ on an English system, values/ — Albanian — as the fallback for everything else).
 */
object LocalePreferences {
    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)

    fun set(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, languageCode)
        }
    }

    fun wrap(context: Context): Context {
        val languageCode = get(context) ?: return context
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
