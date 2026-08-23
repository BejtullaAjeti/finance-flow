package com.example.financeflow.viewmodel

import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryViewModelTest {

    @Test
    fun isLastOfType_trueWhenOnlyOneCategoryOfThatType() {
        val others = Category(id = 1, name = "Others", type = CategoryType.PERSONAL)
        val categories = listOf(others, Category(id = 2, name = "Business Income", type = CategoryType.BUSINESS))

        assertTrue(CategoryViewModel.isLastOfType(categories, others))
    }

    @Test
    fun isLastOfType_falseWhenAnotherCategoryOfTheSameTypeExists() {
        val food = Category(id = 1, name = "Food", type = CategoryType.PERSONAL)
        val others = Category(id = 2, name = "Others", type = CategoryType.PERSONAL)

        assertFalse(CategoryViewModel.isLastOfType(listOf(food, others), others))
    }
}
