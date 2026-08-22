package com.example.financeflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (:type IS NULL OR type = :type) ORDER BY date DESC")
    fun getByType(type: TransactionType?): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions WHERE date BETWEEN :start AND :end " +
            "AND (:type IS NULL OR type = :type) ORDER BY date DESC"
    )
    fun getByTypeAndDateRange(type: TransactionType?, start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:start IS NULL OR date >= :start)
          AND (:end IS NULL OR date <= :end)
          AND (:search IS NULL OR note LIKE '%' || :search || '%')
        ORDER BY date DESC
        """
    )
    fun getFiltered(
        type: TransactionType?,
        categoryId: Long?,
        start: LocalDate?,
        end: LocalDate?,
        search: String?
    ): Flow<List<Transaction>>

    @Query(
        """
        SELECT categoryId, currency, SUM(amount) AS total
        FROM transactions
        WHERE isIncome = 0
          AND (:type IS NULL OR type = :type)
          AND date BETWEEN :start AND :end
        GROUP BY categoryId, currency
        """
    )
    fun getCategoryTotals(type: TransactionType?, start: LocalDate, end: LocalDate): Flow<List<CategoryCurrencyTotal>>

    @Query(
        """
        SELECT date(date * 86400, 'unixepoch') AS bucket, currency,
               SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income,
               SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE date BETWEEN :start AND :end AND (:type IS NULL OR type = :type)
        GROUP BY bucket, currency
        ORDER BY bucket ASC
        """
    )
    fun getDailyTotals(start: LocalDate, end: LocalDate, type: TransactionType?): Flow<List<PeriodCurrencyTotal>>

    // ponytail: %W is SQLite's week-of-year (Sunday-start, not ISO-8601), good enough for a bar-chart
    // bucket; swap for a computed ISO week column if exact week numbering matters later.
    @Query(
        """
        SELECT strftime('%Y-%W', date * 86400, 'unixepoch') AS bucket, currency,
               SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income,
               SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE date BETWEEN :start AND :end AND (:type IS NULL OR type = :type)
        GROUP BY bucket, currency
        ORDER BY bucket ASC
        """
    )
    fun getWeeklyTotals(start: LocalDate, end: LocalDate, type: TransactionType?): Flow<List<PeriodCurrencyTotal>>

    @Query(
        """
        SELECT strftime('%Y-%m', date * 86400, 'unixepoch') AS bucket, currency,
               SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income,
               SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE date BETWEEN :start AND :end AND (:type IS NULL OR type = :type)
        GROUP BY bucket, currency
        ORDER BY bucket ASC
        """
    )
    fun getMonthlyTotals(start: LocalDate, end: LocalDate, type: TransactionType?): Flow<List<PeriodCurrencyTotal>>

    @Query(
        """
        SELECT strftime('%Y', date * 86400, 'unixepoch') AS bucket, currency,
               SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income,
               SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE date BETWEEN :start AND :end AND (:type IS NULL OR type = :type)
        GROUP BY bucket, currency
        ORDER BY bucket ASC
        """
    )
    fun getYearlyTotals(start: LocalDate, end: LocalDate, type: TransactionType?): Flow<List<PeriodCurrencyTotal>>
}
