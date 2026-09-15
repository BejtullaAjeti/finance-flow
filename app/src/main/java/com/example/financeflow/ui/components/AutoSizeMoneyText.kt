package com.example.financeflow.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.round

/**
 * Single-line money/total display that shrinks its font size to fit the available width instead
 * of wrapping — the fix for large totals (e.g. "12.312.444,00 €") breaking mid-number across two
 * lines and overlapping their label. [minFontSize] is the readable floor: if [text] still doesn't
 * fit at that size, this swaps to [abbreviatedText] (e.g. "12,3M €") rather than clipping.
 */
@Composable
fun AutoSizeMoneyText(
    text: String,
    abbreviatedText: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 14.sp,
    // Test-only hook (no-op in production call sites) so tests can assert the resolved layout —
    // e.g. lineCount/didOverflowWidth — without depending on Compose's semantics tree for it.
    onTextLayoutResult: ((TextLayoutResult) -> Unit)? = null
) {
    var useAbbreviated by remember(text) { mutableStateOf(false) }
    val resolvedColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

    BasicText(
        text = if (useAbbreviated) abbreviatedText else text,
        modifier = modifier,
        style = style.copy(color = resolvedColor),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = style.fontSize),
        onTextLayout = { result ->
            if (result.didOverflowWidth && !useAbbreviated) useAbbreviated = true
            onTextLayoutResult?.invoke(result)
        }
    )
}

/**
 * "12,3M €"-style abbreviated form of [amount] using [currencyFormat] for the currency symbol/
 * separators — [AutoSizeMoneyText]'s fallback for values that don't fit even at its font-size
 * floor. Below 1000 this is just the normal formatted amount (nothing to abbreviate).
 */
fun abbreviatedCurrencyText(amount: Double, currencyFormat: NumberFormat): String {
    val absAmount = abs(amount)
    val (divisor, suffix) = when {
        absAmount >= 1_000_000_000_000.0 -> 1_000_000_000_000.0 to "T"
        absAmount >= 1_000_000_000.0 -> 1_000_000_000.0 to "B"
        absAmount >= 1_000_000.0 -> 1_000_000.0 to "M"
        absAmount >= 1_000.0 -> 1_000.0 to "K"
        else -> return currencyFormat.format(amount)
    }
    val scaled = amount / divisor
    val scaledFormat = (currencyFormat.clone() as NumberFormat).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = if (scaled == round(scaled)) 0 else 1
    }
    val formatted = scaledFormat.format(scaled)
    // Insert the magnitude suffix right after the number, wherever the currency symbol/spacing
    // put it (prefix "$12.3" -> "$12.3M", suffix "12,3 €" -> "12,3M €").
    val lastDigitIndex = formatted.indexOfLast { it.isDigit() }
    return if (lastDigitIndex == -1) formatted
    else formatted.substring(0, lastDigitIndex + 1) + suffix + formatted.substring(lastDigitIndex + 1)
}

/**
 * For a net/balance figure (income minus expense) — explicit "+"-prefix for a non-negative
 * amount, since [currencyFormat] only ever marks negatives on its own. Pairs with
 * [signedAbbreviatedCurrencyText] for [AutoSizeMoneyText]'s floor fallback.
 */
fun signedCurrencyText(amount: Double, currencyFormat: NumberFormat): String =
    if (amount >= 0) "+" + currencyFormat.format(amount) else currencyFormat.format(amount)

fun signedAbbreviatedCurrencyText(amount: Double, currencyFormat: NumberFormat): String =
    if (amount >= 0) "+" + abbreviatedCurrencyText(amount, currencyFormat) else abbreviatedCurrencyText(amount, currencyFormat)
