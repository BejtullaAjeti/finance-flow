package com.example.financeflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<Category>>

    // ponytail: a category tagged BOTH always matches, since it applies to Personal and Business alike.
    @Query("SELECT * FROM categories WHERE :type IS NULL OR type = :type OR type = 'BOTH' ORDER BY name ASC")
    fun getByType(type: CategoryType?): Flow<List<Category>>
}
