package com.example.financeflow.data

data class PeriodTotal(
    val bucket: String,
    val income: Double,
    val expense: Double
)

data class CategoryTotal(
    val categoryId: Long,
    val total: Double
)

data class PeriodCurrencyTotal(
    val bucket: String,
    val currency: Currency,
    val income: Double,
    val expense: Double
)

data class CategoryCurrencyTotal(
    val categoryId: Long,
    val currency: Currency,
    val total: Double
)

enum class ReportPeriod { DAY, WEEK, MONTH, YEAR }
