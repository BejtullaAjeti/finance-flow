package com.example.financeflow.data.backup

import androidx.room.withTransaction
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.RecurringRuleRepository
import com.example.financeflow.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first

/**
 * The app's only backup/restore path (CLAUDE.md §9) since there's no cloud sync. Restoring is a
 * full replace, not a merge — the only way to keep imported foreign keys (categoryId,
 * recurringId) valid without remapping IDs.
 */
class BackupManager(private val db: AppDatabase) {
    private val categoryRepository = CategoryRepository(db.categoryDao())
    private val recurringRuleRepository = RecurringRuleRepository(db.recurringRuleDao())
    private val transactionRepository = TransactionRepository(db.transactionDao())

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        categories = categoryRepository.getAll().first(),
        recurringRules = recurringRuleRepository.getAll().first(),
        transactions = transactionRepository.getAll().first()
    )

    suspend fun importPayload(payload: BackupPayload) {
        db.withTransaction {
            // Children first (FK-referencing tables), so the RESTRICT/SET_NULL constraints on
            // categoryId/recurringId never see a dangling reference mid-wipe.
            transactionRepository.deleteAll()
            recurringRuleRepository.deleteAll()
            categoryRepository.deleteAll()

            categoryRepository.insertAll(payload.categories)
            recurringRuleRepository.insertAll(payload.recurringRules)
            transactionRepository.insertAll(payload.transactions)
        }
    }
}
