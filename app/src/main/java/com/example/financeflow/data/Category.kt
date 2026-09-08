package com.example.financeflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType { PERSONAL, BUSINESS, BOTH }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Non-null only for the categories seeded on first launch (see MainActivity.defaultCategories)
    // — a stable, locale-independent key ("food", "salary", ...) resolved to the current locale's
    // string at display time via Category.displayName(), instead of the frozen-at-seed-time
    // [name]. Null for every user-created category, whose [name] is their own words and must be
    // shown verbatim, never run through the resource lookup.
    val nameKey: String? = null,
    val type: CategoryType,
    val budgetLimit: Double? = null,
    val budgetLimitCurrency: Currency = Currency.EUR,
    val budgetPeriod: ReportPeriod = ReportPeriod.MONTH,
    val icon: String? = null,
    val color: String? = null
)
