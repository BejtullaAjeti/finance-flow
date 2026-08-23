package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.example.financeflow.R
import com.example.financeflow.locale.currentAppLocale
import com.example.financeflow.ui.theme.Radius
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** First cell of a 6x7 month grid: the month's first day, walked back to the week's start. */
fun calendarGridStart(month: YearMonth, firstDayOfWeek: DayOfWeek): LocalDate {
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    return firstOfMonth.minusDays(leadingBlanks.toLong())
}

/**
 * Flat, app-styled replacement for M3's stock DatePickerDialog — java.time + Compose primitives
 * only, no new dependency (spec §8). Container follows the same policy as every other dialog:
 * Surface background, Radius.large corners, no border, no shadow.
 */
@Composable
fun CalendarDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = currentAppLocale()
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.large))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Icon(PhosphorIcons.Regular.CaretLeft, contentDescription = stringResource(R.string.calendar_previous_month))
                }
                Text(
                    text = displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.titlecase(locale) } + " " + displayedMonth.year,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                    Icon(PhosphorIcons.Regular.CaretRight, contentDescription = stringResource(R.string.calendar_next_month))
                }
            }

            Spacer(Modifier.height(8.dp))

            val orderedDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plus(it.toLong()) } }
            Row(modifier = Modifier.fillMaxWidth()) {
                orderedDays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val gridStart = remember(displayedMonth, firstDayOfWeek) { calendarGridStart(displayedMonth, firstDayOfWeek) }
            val today = remember { LocalDate.now() }

            for (week in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in 0 until 7) {
                        val day = gridStart.plusDays((week * 7 + dow).toLong())
                        val inMonth = YearMonth.from(day) == displayedMonth
                        val isSelected = day == selectedDate
                        val isToday = day == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    selectedDate = day
                                    if (!inMonth) displayedMonth = YearMonth.from(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                ConfirmButton(enabled = true, onClick = { onConfirm(selectedDate) })
            }
        }
    }
}
