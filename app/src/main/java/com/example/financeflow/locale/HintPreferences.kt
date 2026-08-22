package com.example.financeflow.locale

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "settings"
private const val KEY_CATEGORY_PICKER_HINT_DISMISSED = "hint_category_picker_dismissed"

/**
 * First-use hints (as opposed to empty-state hints, which just re-derive from the list being
 * empty and need no persistence) need a flag that survives across app launches, so dismissing
 * one is permanent rather than "until the screen is reopened."
 */
object HintPreferences {
    fun isCategoryPickerHintDismissed(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CATEGORY_PICKER_HINT_DISMISSED, false)

    fun dismissCategoryPickerHint(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_CATEGORY_PICKER_HINT_DISMISSED, true)
        }
    }
}
