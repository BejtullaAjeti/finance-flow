package com.example.financeflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rate_cache WHERE id = 0")
    fun observe(): Flow<ExchangeRateCache?>

    @Query("SELECT * FROM exchange_rate_cache WHERE id = 0")
    suspend fun getOnce(): ExchangeRateCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: ExchangeRateCache)
}
