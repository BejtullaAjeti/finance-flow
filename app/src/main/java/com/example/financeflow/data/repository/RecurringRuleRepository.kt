package com.example.financeflow.data.repository

import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.RecurringRuleDao
import com.example.financeflow.data.TransactionType
import kotlinx.coroutines.flow.Flow

class RecurringRuleRepository(private val dao: RecurringRuleDao) {
    fun getAll(): Flow<List<RecurringRule>> = dao.getAll()

    fun getByType(type: TransactionType?): Flow<List<RecurringRule>> = dao.getByType(type)

    suspend fun insert(rule: RecurringRule): Long = dao.insert(rule)

    suspend fun insertAll(rules: List<RecurringRule>) = dao.insertAll(rules)

    suspend fun update(rule: RecurringRule) = dao.update(rule)

    suspend fun delete(rule: RecurringRule) = dao.delete(rule)

    suspend fun deleteAll() = dao.deleteAll()
}
