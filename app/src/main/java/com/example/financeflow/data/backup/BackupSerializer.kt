package com.example.financeflow.data.backup

import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private const val SCHEMA_VERSION = 1

data class BackupPayload(
    val categories: List<Category>,
    val recurringRules: List<RecurringRule>,
    val transactions: List<Transaction>
)

fun BackupPayload.toJson(): String {
    val root = JSONObject()
    root.put("version", SCHEMA_VERSION)
    root.put("categories", JSONArray(categories.map { it.toJson() }))
    root.put("recurringRules", JSONArray(recurringRules.map { it.toJson() }))
    root.put("transactions", JSONArray(transactions.map { it.toJson() }))
    return root.toString(2)
}

fun parseBackup(json: String): BackupPayload {
    val root = JSONObject(json)
    return BackupPayload(
        categories = root.getJSONArray("categories").mapObjects { it.toCategory() },
        recurringRules = root.getJSONArray("recurringRules").mapObjects { it.toRecurringRule() },
        transactions = root.getJSONArray("transactions").mapObjects { it.toTransaction() }
    )
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun Category.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("type", type.name)
    put("budgetLimit", budgetLimit ?: JSONObject.NULL)
    put("budgetLimitCurrency", budgetLimitCurrency.name)
    put("icon", icon ?: JSONObject.NULL)
    put("color", color ?: JSONObject.NULL)
}

private fun JSONObject.toCategory(): Category = Category(
    id = getLong("id"),
    name = getString("name"),
    type = CategoryType.valueOf(getString("type")),
    budgetLimit = optDoubleOrNull("budgetLimit"),
    budgetLimitCurrency = Currency.valueOf(getString("budgetLimitCurrency")),
    icon = optStringOrNull("icon"),
    color = optStringOrNull("color")
)

private fun RecurringRule.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("label", label)
    put("amount", amount)
    put("currency", currency.name)
    put("categoryId", categoryId)
    put("type", type.name)
    put("isIncome", isIncome)
    put("frequency", frequency.name)
    put("customIntervalDays", customIntervalDays ?: JSONObject.NULL)
    put("nextDueDate", nextDueDate.toString())
    put("active", active)
}

private fun JSONObject.toRecurringRule(): RecurringRule = RecurringRule(
    id = getLong("id"),
    label = getString("label"),
    amount = getDouble("amount"),
    currency = Currency.valueOf(getString("currency")),
    categoryId = getLong("categoryId"),
    type = TransactionType.valueOf(getString("type")),
    isIncome = getBoolean("isIncome"),
    frequency = Frequency.valueOf(getString("frequency")),
    customIntervalDays = optIntOrNull("customIntervalDays"),
    nextDueDate = LocalDate.parse(getString("nextDueDate")),
    active = getBoolean("active")
)

private fun Transaction.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("amount", amount)
    put("currency", currency.name)
    put("type", type.name)
    put("categoryId", categoryId)
    put("date", date.toString())
    put("note", note ?: JSONObject.NULL)
    put("isIncome", isIncome)
    put("recurringId", recurringId ?: JSONObject.NULL)
}

private fun JSONObject.toTransaction(): Transaction = Transaction(
    id = getLong("id"),
    amount = getDouble("amount"),
    currency = Currency.valueOf(getString("currency")),
    type = TransactionType.valueOf(getString("type")),
    categoryId = getLong("categoryId"),
    date = LocalDate.parse(getString("date")),
    note = optStringOrNull("note"),
    isIncome = getBoolean("isIncome"),
    recurringId = optLongOrNull("recurringId")
)

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)
private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
