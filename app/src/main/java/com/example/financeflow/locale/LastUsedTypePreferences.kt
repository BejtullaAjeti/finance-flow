package com.example.financeflow.locale

import android.content.Context
import androidx.core.content.edit
import com.example.financeflow.data.TransactionType

private const val PREFS_NAME = "settings"
private const val KEY_LAST_USED_TYPE = "last_used_transaction_type"

/**
 * Remembers the Personal/Business type of the last saved transaction, so a new transaction opened
 * from a neutral (Combined) context defaults to whatever was last used instead of always PERSONAL.
 */
object LastUsedTypePreferences {
    fun get(context: Context): TransactionType =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_USED_TYPE, null)
            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            ?: TransactionType.PERSONAL

    fun set(context: Context, type: TransactionType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_USED_TYPE, type.name)
        }
    }
}
