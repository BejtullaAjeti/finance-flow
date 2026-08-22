package com.example.financeflow.data.repository

import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.PeriodCurrencyTotal
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionDao
import com.example.financeflow.data.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TransactionRepository(private val dao: TransactionDao) {
    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        dao.getByDateRange(start, end)

    fun getByType(type: TransactionType?): Flow<List<Transaction>> = dao.getByType(type)

    fun getByTypeAndDateRange(type: TransactionType?, start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        dao.getByTypeAndDateRange(type, start, end)

    fun getFiltered(
        type: TransactionType?,
        categoryId: Long?,
        start: LocalDate?,
        end: LocalDate?,
        search: String?
    ): Flow<List<Transaction>> = dao.getFiltered(type, categoryId, start, end, search)

    suspend fun insert(transaction: Transaction): Long = dao.insert(transaction)

    suspend fun insertAll(transactions: List<Transaction>) = dao.insertAll(transactions)

    suspend fun update(transaction: Transaction) = dao.update(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)

    suspend fun deleteAll() = dao.deleteAll()

    fun getTotals(type: TransactionType?, period: ReportPeriod, start: LocalDate, end: LocalDate): Flow<List<PeriodCurrencyTotal>> =
        when (period) {
            ReportPeriod.DAY -> dao.getDailyTotals(start, end, type)
            ReportPeriod.WEEK -> dao.getWeeklyTotals(start, end, type)
            ReportPeriod.MONTH -> dao.getMonthlyTotals(start, end, type)
            ReportPeriod.YEAR -> dao.getYearlyTotals(start, end, type)
        }

    fun getCategoryTotals(type: TransactionType?, start: LocalDate, end: LocalDate): Flow<List<CategoryCurrencyTotal>> =
        dao.getCategoryTotals(type, start, end)
}
