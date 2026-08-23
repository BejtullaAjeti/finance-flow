package com.example.financeflow.data.repository

import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryDao
import com.example.financeflow.data.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(private val dao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = dao.getAll()

    fun getByType(type: CategoryType?): Flow<List<Category>> = dao.getByType(type)

    // ponytail: without this, a fresh install has zero categories, the Add/Edit Transaction
    // picker is permanently empty, and its save button can never satisfy selectedCategory != null.
    suspend fun seedDefaultsIfEmpty(defaults: List<Category>) {
        if (dao.getAll().first().isEmpty()) {
            dao.insertAll(defaults)
        }
    }

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun insertAll(categories: List<Category>) = dao.insertAll(categories)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    suspend fun deleteAll() = dao.deleteAll()
}
