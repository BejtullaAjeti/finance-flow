package com.example.financeflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Transaction
import com.example.financeflow.ui.theme.Expense
import com.example.financeflow.ui.theme.Income
import com.example.financeflow.ui.theme.MoneyFigure
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Composable
fun TransactionRow(
    transaction: Transaction,
    categoryName: String,
    displayAmount: Double,
    currencyFormat: NumberFormat,
    dateFormat: DateTimeFormatter,
    displayCurrency: Currency,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = categoryName, style = MaterialTheme.typography.bodyLarge)
            Text(text = transaction.date.format(dateFormat), style = MaterialTheme.typography.bodyMedium)
            if (transaction.currency != displayCurrency) {
                Text(
                    text = "${transaction.currency.name} ${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        val amountColor = if (transaction.isIncome) Income else Expense
        val sign = if (transaction.isIncome) "+" else "-"
        Text(
            text = "$sign${currencyFormat.format(displayAmount)}",
            style = MoneyFigure,
            color = amountColor
        )
    }
}
