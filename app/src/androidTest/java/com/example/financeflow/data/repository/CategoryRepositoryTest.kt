package com.example.financeflow.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CategoryRepository

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = CategoryRepository(db.categoryDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun seedDefaultsIfEmpty_insertsDefaultsOnFreshDatabase() = runBlocking {
        repository.seedDefaultsIfEmpty(listOf(Category(name = "Food", type = CategoryType.PERSONAL)))

        assertEquals(listOf("Food"), db.categoryDao().getAll().first().map { it.name })
    }

    @Test
    fun seedDefaultsIfEmpty_doesNothingWhenCategoriesAlreadyExist() = runBlocking {
        db.categoryDao().insert(Category(name = "Existing", type = CategoryType.PERSONAL))

        repository.seedDefaultsIfEmpty(listOf(Category(name = "Food", type = CategoryType.PERSONAL)))

        assertEquals(listOf("Existing"), db.categoryDao().getAll().first().map { it.name })
    }
}
