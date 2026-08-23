package com.example.financeflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType { PERSONAL, BUSINESS, BOTH }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val budgetLimit: Double? = null,
    val budgetLimitCurrency: Currency = Currency.EUR,
    val icon: String? = null,
    val color: String? = null
)
