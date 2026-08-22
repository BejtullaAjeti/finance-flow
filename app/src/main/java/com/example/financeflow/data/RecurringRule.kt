package com.example.financeflow.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Frequency { WEEKLY, MONTHLY, CUSTOM }

@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId")]
)
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amount: Double,
    val currency: Currency = Currency.MKD,
    val categoryId: Long,
    val type: TransactionType,
    val isIncome: Boolean,
    val frequency: Frequency,
    // ponytail: only used when frequency == CUSTOM; not enforced at the type level
    val customIntervalDays: Int? = null,
    val nextDueDate: LocalDate,
    val active: Boolean = true
)
