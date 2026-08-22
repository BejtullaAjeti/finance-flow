package com.example.financeflow.recurring

import androidx.room.withTransaction
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.dueOccurrences
import com.example.financeflow.data.repository.RecurringRuleRepository
import com.example.financeflow.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * The CLAUDE.md §9 auto-generation step: on launch (or from a background WorkManager check),
 * roll every active due rule forward and insert the corresponding transactions.
 */
class RecurringRuleProcessor(private val db: AppDatabase) {
    private val recurringRuleRepository = RecurringRuleRepository(db.recurringRuleDao())
    private val transactionRepository = TransactionRepository(db.transactionDao())

    suspend fun generateDueTransactions(today: LocalDate = LocalDate.now()) {
        val dueRules = recurringRuleRepository.getAll().first().filter { it.active && it.nextDueDate <= today }

        for (rule in dueRules) {
            val occurrences = dueOccurrences(rule, today)
            if (occurrences.isEmpty()) continue

            // Wrapped per rule: a crash mid-batch rolls back that rule's partial work instead of
            // double-generating transactions or losing the nextDueDate advance on the next run.
            db.withTransaction {
                occurrences.forEach { occurrence ->
                    transactionRepository.insert(
                        Transaction(
                            amount = rule.amount,
                            currency = rule.currency,
                            type = rule.type,
                            categoryId = rule.categoryId,
                            date = occurrence.dueDate,
                            note = rule.label,
                            isIncome = rule.isIncome,
                            recurringId = rule.id
                        )
                    )
                }
                recurringRuleRepository.update(occurrences.last().updatedRule)
            }
        }
    }
}
