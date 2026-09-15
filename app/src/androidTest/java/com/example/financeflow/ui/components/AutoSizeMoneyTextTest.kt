package com.example.financeflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.FinanceFlowTheme
import com.example.financeflow.ui.theme.MoneyFigureLarge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Regression coverage for the "This Month" card number overflow bug: a large total (originally
 * "12 312 444,00 €") wrapped onto a second line and overlapped its label instead of staying on
 * one line. Reproduces the card's actual layout shape — a phone-width GlassCard's content width
 * (screen padding + card padding subtracted), split into two SpaceBetween weight(1f) halves —
 * and drives the exact bug value plus larger ones through [AutoSizeMoneyText].
 */
class AutoSizeMoneyTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    // sq-AL/EUR is what produced the original "12 312 444,00 €" formatting.
    private val albanianEuros: NumberFormat = NumberFormat.getCurrencyInstance(Locale("sq", "AL")).apply {
        currency = Currency.getInstance("EUR")
    }

    // HomeScreen's outer Column padding (16dp each side) + GlassCard's content padding (20dp
    // each side) eaten out of a 360dp-wide phone — the real width the income/expense Row gets.
    private val cardContentWidth = 360.dp - 32.dp - 40.dp

    private fun assertOneLineNoOverflow(amount: Double) {
        var layout: TextLayoutResult? = null
        composeRule.setContent {
            FinanceFlowTheme {
                Row(modifier = Modifier.width(cardContentWidth), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Income", style = MaterialTheme.typography.bodyMedium)
                        AutoSizeMoneyText(
                            text = albanianEuros.format(amount),
                            abbreviatedText = abbreviatedCurrencyText(amount, albanianEuros),
                            style = MoneyFigureLarge,
                            onTextLayoutResult = { layout = it }
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {}
                }
            }
        }
        composeRule.waitForIdle()

        val result = requireNotNull(layout) { "onTextLayoutResult never fired for amount=$amount" }
        assertEquals(
            "wrapped onto more than one line for amount=$amount, rendered as \"${result.layoutInput.text}\"",
            1,
            result.lineCount
        )
        assertFalse(
            "still overflowed the available width for amount=$amount, rendered as \"${result.layoutInput.text}\"",
            result.didOverflowWidth
        )
    }

    @Test
    fun exactBugValue_staysOnOneLine() {
        // The exact value from the report: "12 312 444,00 €".
        assertOneLineNoOverflow(12_312_444.00)
    }

    @Test
    fun hundredsOfMillions_staysOnOneLine() {
        assertOneLineNoOverflow(987_654_321.00)
    }

    @Test
    fun billions_staysOnOneLine() {
        assertOneLineNoOverflow(12_345_678_901.00)
    }

    @Test
    fun trillions_fallsBackToAbbreviatedAndStillFits() {
        // Large enough that even the font-size floor can't fit the full number — this is what
        // exercises the abbreviated-text fallback rather than the shrink-to-fit path alone.
        assertOneLineNoOverflow(9_999_999_999_999.00)
    }
}
