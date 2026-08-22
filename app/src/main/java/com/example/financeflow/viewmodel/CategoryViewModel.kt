package com.example.financeflow.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val typeFilter = MutableStateFlow<CategoryType?>(null)

    val filteredCategories: StateFlow<List<Category>> = typeFilter
        .flatMapLatest { repository.getByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: CategoryType?) {
        typeFilter.value = type
    }

    fun addCategory(category: Category, onInserted: (Long) -> Unit = {}) {
        viewModelScope.launch { onInserted(repository.insert(category)) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.update(category) }
    }

    // ponytail: Transaction.categoryId is a RESTRICT foreign key, so deleting a category still
    // referenced by transactions throws — caught here so callers can surface it instead of crashing.
    // The last-of-type check is a separate, non-DB guard: a transaction always needs a category to
    // belong to, so the last category for a type must survive even when nothing references it yet.
    fun deleteCategory(
        category: Category,
        onDeleted: () -> Unit = {},
        onBlocked: (CategoryDeleteBlockReason) -> Unit = {}
    ) {
        if (isLastOfType(categories.value, category)) {
            onBlocked(CategoryDeleteBlockReason.LAST_OF_TYPE)
            return
        }
        viewModelScope.launch {
            try {
                repository.delete(category)
                onDeleted()
            } catch (e: SQLiteConstraintException) {
                onBlocked(CategoryDeleteBlockReason.IN_USE)
            }
        }
    }

    companion object {
        fun isLastOfType(categories: List<Category>, category: Category): Boolean =
            categories.count { it.type == category.type } <= 1
    }
}

enum class CategoryDeleteBlockReason { IN_USE, LAST_OF_TYPE }
