package com.example.financeflow.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getByTypeIncludesBothCategories() = runBlocking {
        db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))
        db.categoryDao().insert(Category(name = "Client Work", type = CategoryType.BUSINESS))
        db.categoryDao().insert(Category(name = "Bank Fees", type = CategoryType.BOTH))

        val personal = db.categoryDao().getByType(CategoryType.PERSONAL).first()
        assertEquals(setOf("Groceries", "Bank Fees"), personal.map { it.name }.toSet())

        val business = db.categoryDao().getByType(CategoryType.BUSINESS).first()
        assertEquals(setOf("Client Work", "Bank Fees"), business.map { it.name }.toSet())

        assertEquals(3, db.categoryDao().getByType(null).first().size)
    }
}
