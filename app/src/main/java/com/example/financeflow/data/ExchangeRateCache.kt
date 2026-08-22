package com.example.financeflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single-row cache (id is always 0) of the last successfully fetched USD-pivoted rates. Rates
// default to 1.0 (a neutral fallback, not a crash or a blocked UI) until the first fetch succeeds.
@Entity(tableName = "exchange_rate_cache")
data class ExchangeRateCache(
    @PrimaryKey val id: Int = 0,
    val rateEur: Double = 1.0,
    val rateMkd: Double = 1.0,
    val rateChf: Double = 1.0,
    val lastUpdatedEpochMillis: Long? = null
)
