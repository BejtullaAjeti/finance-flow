package com.example.financeflow.data.backup

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whether any backed-up table (transactions, categories, recurring_rules) has changed since the
 * last successful backup of any kind (daily, on-close, or manual). AppDatabase wires this to
 * Room's own InvalidationTracker on every write, so it needs no changes to the repositories
 * themselves. Cleared whenever a backup write succeeds; the on-close trigger uses it to skip
 * writing a duplicate backup when nothing changed since the last one.
 */
object BackupDirtyTracker {
    // Starts dirty so the very first close after configuring a backup folder still backs up.
    private val dirty = AtomicBoolean(true)

    fun markDirty() { dirty.set(true) }
    fun markClean() { dirty.set(false) }
    fun isDirty(): Boolean = dirty.get()
}
