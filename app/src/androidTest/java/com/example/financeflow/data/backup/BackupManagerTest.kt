package com.example.financeflow.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.Transaction
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
class BackupManagerTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        manager = BackupManager(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun exportThenImport_restoresExactData() = runBlocking {
        val categoryId = db.categoryDao().insert(Category(name = "Groceries", type = CategoryType.PERSONAL, budgetLimit = 300.0))
        val ruleId = db.recurringRuleDao().insert(
            RecurringRule(
                label = "Rent", amount = 500.0, categoryId = categoryId, type = TransactionType.PERSONAL,
                isIncome = false, frequency = Frequency.MONTHLY, nextDueDate = LocalDate.of(2026, 9, 1)
            )
        )
        db.transactionDao().insert(
            Transaction(
                amount = 42.5, type = TransactionType.PERSONAL, categoryId = categoryId,
                date = LocalDate.of(2026, 8, 1), note = "milk", isIncome = false, recurringId = ruleId
            )
        )

        val payload = manager.exportPayload()

        // Simulate restoring onto a device with different existing data.
        db.categoryDao().insert(Category(name = "Stale", type = CategoryType.BUSINESS))

        manager.importPayload(payload)

        val categories = db.categoryDao().getAll().first()
        val rules = db.recurringRuleDao().getAll().first()
        val transactions = db.transactionDao().getAll().first()

        assertEquals(listOf("Groceries"), categories.map { it.name })
        assertEquals(1, rules.size)
        assertEquals(1, transactions.size)
        assertEquals("milk", transactions[0].note)
        assertEquals(ruleId, transactions[0].recurringId)
    }

    @Test
    fun import_replacesExistingDataEntirely() = runBlocking {
        db.categoryDao().insert(Category(name = "Old Category", type = CategoryType.PERSONAL))
        val emptyPayload = BackupPayload(emptyList(), emptyList(), emptyList())

        manager.importPayload(emptyPayload)

        assertTrue(db.categoryDao().getAll().first().isEmpty())
    }
}
