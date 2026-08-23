package com.example.financeflow.recurring

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecurringRuleProcessorTest {
    private lateinit var db: AppDatabase
    private lateinit var processor: RecurringRuleProcessor
    private var categoryId: Long = 0

    @Before
    fun createDb() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        processor = RecurringRuleProcessor(db)
        categoryId = db.categoryDao().insert(Category(name = "Rent", type = CategoryType.PERSONAL))
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun generatesTransactionAndAdvancesNextDueDate_whenDue() = runBlocking {
        val today = LocalDate.of(2026, 8, 22)
        val ruleId = db.recurringRuleDao().insert(
            RecurringRule(
                label = "Rent", amount = 500.0, categoryId = categoryId, type = TransactionType.PERSONAL,
                isIncome = false, frequency = Frequency.MONTHLY, nextDueDate = today
            )
        )

        processor.generateDueTransactions(today)

        val transactions = db.transactionDao().getAll().first()
        assertEquals(1, transactions.size)
        assertEquals(500.0, transactions[0].amount, 0.0)
        assertEquals(today, transactions[0].date)
        assertEquals(ruleId, transactions[0].recurringId)

        val updatedRule = db.recurringRuleDao().getAll().first().first { it.id == ruleId }
        assertEquals(LocalDate.of(2026, 9, 22), updatedRule.nextDueDate)
    }

    @Test
    fun doesNothing_whenNotYetDue() = runBlocking {
        val today = LocalDate.of(2026, 8, 22)
        db.recurringRuleDao().insert(
            RecurringRule(
                label = "Rent", amount = 500.0, categoryId = categoryId, type = TransactionType.PERSONAL,
                isIncome = false, frequency = Frequency.MONTHLY, nextDueDate = LocalDate.of(2026, 9, 1)
            )
        )

        processor.generateDueTransactions(today)

        assertTrue(db.transactionDao().getAll().first().isEmpty())
    }

    @Test
    fun skipsPausedRules() = runBlocking {
        val today = LocalDate.of(2026, 8, 22)
        db.recurringRuleDao().insert(
            RecurringRule(
                label = "Rent", amount = 500.0, categoryId = categoryId, type = TransactionType.PERSONAL,
                isIncome = false, frequency = Frequency.MONTHLY, nextDueDate = today, active = false
            )
        )

        processor.generateDueTransactions(today)

        assertTrue(db.transactionDao().getAll().first().isEmpty())
    }

    @Test
    fun catchesUpMultipleMissedOccurrences() = runBlocking {
        val today = LocalDate.of(2026, 8, 22)
        val ruleId = db.recurringRuleDao().insert(
            RecurringRule(
                label = "Weekly allowance", amount = 20.0, categoryId = categoryId, type = TransactionType.PERSONAL,
                isIncome = true, frequency = Frequency.WEEKLY, nextDueDate = LocalDate.of(2026, 8, 1)
            )
        )

        processor.generateDueTransactions(today)

        val transactions = db.transactionDao().getAll().first()
        assertEquals(4, transactions.size)
        assertTrue(transactions.all { it.recurringId == ruleId && it.isIncome })

        val updatedRule = db.recurringRuleDao().getAll().first().first { it.id == ruleId }
        assertEquals(LocalDate.of(2026, 8, 29), updatedRule.nextDueDate)
    }
}
