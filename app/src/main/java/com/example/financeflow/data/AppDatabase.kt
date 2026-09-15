package com.example.financeflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.financeflow.data.backup.BackupDirtyTracker

@Database(
    entities = [Transaction::class, Category::class, RecurringRule::class, ExchangeRateCache::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financeflow.db"
                )
                    // No shipped migration path exists yet and the app hasn't released, so a
                    // destructive fallback is acceptable for the schema bump this version adds.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { db ->
                        // Rides Room's own write-notification mechanism (the same one that
                        // refreshes every Flow-returning DAO query) instead of touching each
                        // repository's insert/update/delete calls individually.
                        db.invalidationTracker.addObserver(
                            object : InvalidationTracker.Observer("transactions", "categories", "recurring_rules") {
                                override fun onInvalidated(tables: Set<String>) = BackupDirtyTracker.markDirty()
                            }
                        )
                        INSTANCE = db
                    }
            }
    }
}
