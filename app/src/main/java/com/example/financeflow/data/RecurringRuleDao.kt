package com.example.financeflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Insert
    suspend fun insert(rule: RecurringRule): Long

    @Insert
    suspend fun insertAll(rules: List<RecurringRule>)

    @Update
    suspend fun update(rule: RecurringRule)

    @Delete
    suspend fun delete(rule: RecurringRule)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()

    @Query("SELECT * FROM recurring_rules ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules WHERE (:type IS NULL OR type = :type) ORDER BY nextDueDate ASC")
    fun getByType(type: TransactionType?): Flow<List<RecurringRule>>
}
