package com.example.financeflow.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.financeflow.data.Category
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Transaction
import com.example.financeflow.ui.theme.MoneyFigure
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    uncategorizedName: String,
    displayAmount: Double,
    currencyFormat: NumberFormat,
    dateFormat: DateTimeFormatter,
    displayCurrency: Currency,
    onClick: () -> Unit,
    showTypeIndicator: Boolean = false
) {
    val amountColor = if (transaction.isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    val sign = if (transaction.isIncome) "+" else "-"

    ListRow(
        icon = CategoryIcons.resolve(category?.icon),
        swatchColor = category?.color.toCategoryColor(),
        title = category?.displayName() ?: uncategorizedName,
        titleTrailingIcon = if (showTypeIndicator) transaction.type.indicatorIcon() else null,
        onClick = onClick,
        subtitle = {
            Text(
                text = transaction.date.format(dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transaction.currency != displayCurrency) {
                Text(
                    text = "${transaction.currency.name} ${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        trailing = {
            Text(
                text = "$sign${currencyFormat.format(displayAmount)}",
                style = MoneyFigure,
                color = amountColor
            )
        }
    )
}
