package com.example.financeflow.data

data class CategoryTotal(
    val categoryId: Long,
    val income: Double,
    val expense: Double
)

data class CategoryCurrencyTotal(
    val categoryId: Long,
    val currency: Currency,
    val income: Double,
    val expense: Double
)

enum class ReportPeriod { DAY, WEEK, MONTH, YEAR }
