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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
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
    fun insertGetAllAndDateRange() = runBlocking {
        val categoryId = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))

        db.transactionDao().insert(
            Transaction(amount = 42.5, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 8, 1), isIncome = false)
        )
        db.transactionDao().insert(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 9, 1), isIncome = false)
        )

        assertEquals(2, db.transactionDao().getAll().first().size)
        assertEquals(1, db.transactionDao().getByDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).first().size)
    }

    @Test
    fun updateAndDelete() = runBlocking {
        val categoryId = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))
        val id = db.transactionDao().insert(
            Transaction(amount = 10.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 8, 1), isIncome = false)
        )
        val transaction = db.transactionDao().getAll().first().first()

        db.transactionDao().update(transaction.copy(id = id, amount = 25.0))
        assertEquals(25.0, db.transactionDao().getAll().first().first().amount, 0.0)

        db.transactionDao().delete(transaction.copy(id = id))
        assertEquals(0, db.transactionDao().getAll().first().size)
    }

    @Test
    fun getByTypeFiltersByTransactionType() = runBlocking {
        val categoryId = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))
        db.transactionDao().insert(
            Transaction(amount = 10.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 8, 1), isIncome = false)
        )
        db.transactionDao().insert(
            Transaction(amount = 20.0, type = TransactionType.BUSINESS, categoryId = categoryId, date = LocalDate.of(2026, 8, 2), isIncome = false)
        )

        assertEquals(1, db.transactionDao().getByType(TransactionType.PERSONAL).first().size)
        assertEquals(1, db.transactionDao().getByType(TransactionType.BUSINESS).first().size)
        assertEquals(2, db.transactionDao().getByType(null).first().size)
    }

    @Test
    fun monthlyTotalsGroupIncomeAndExpenseByBucket() = runBlocking {
        val categoryId = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))
        db.transactionDao().insert(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 8, 1), isIncome = true)
        )
        db.transactionDao().insert(
            Transaction(amount = 30.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 8, 15), isIncome = false)
        )
        db.transactionDao().insert(
            Transaction(amount = 50.0, type = TransactionType.PERSONAL, categoryId = categoryId, date = LocalDate.of(2026, 9, 1), isIncome = false)
        )

        val totals = db.transactionDao().getMonthlyTotals(
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30), null
        ).first()

        assertEquals(2, totals.size)
        val august = totals.first { it.bucket == "2026-08" }
        assertEquals(100.0, august.income, 0.0)
        assertEquals(30.0, august.expense, 0.0)
        val september = totals.first { it.bucket == "2026-09" }
        assertEquals(0.0, september.income, 0.0)
        assertEquals(50.0, september.expense, 0.0)
    }

    @Test
    fun getFilteredCombinesTypeCategoryDateAndSearch() = runBlocking {
        val groceries = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL))
        val travel = db.categoryDao().insert(Category(name = "Travel", type = CategoryType.PERSONAL))
        db.transactionDao().insert(
            Transaction(amount = 20.0, type = TransactionType.PERSONAL, categoryId = groceries, date = LocalDate.of(2026, 8, 1), isIncome = false, note = "milk and eggs")
        )
        db.transactionDao().insert(
            Transaction(amount = 40.0, type = TransactionType.PERSONAL, categoryId = travel, date = LocalDate.of(2026, 8, 2), isIncome = false, note = "train ticket")
        )
        db.transactionDao().insert(
            Transaction(amount = 15.0, type = TransactionType.BUSINESS, categoryId = groceries, date = LocalDate.of(2026, 8, 1), isIncome = false, note = "milk for office")
        )

        // no filters: everything
        assertEquals(3, db.transactionDao().getFiltered(null, null, null, null, null).first().size)

        // type only
        assertEquals(2, db.transactionDao().getFiltered(TransactionType.PERSONAL, null, null, null, null).first().size)

        // type + category
        assertEquals(1, db.transactionDao().getFiltered(TransactionType.PERSONAL, groceries, null, null, null).first().size)

        // date range excluding the second day
        assertEquals(
            2,
            db.transactionDao().getFiltered(null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null).first().size
        )

        // note search, case-sensitive substring per SQLite LIKE default collation on ASCII
        assertEquals(2, db.transactionDao().getFiltered(null, null, null, null, "milk").first().size)
    }
}
