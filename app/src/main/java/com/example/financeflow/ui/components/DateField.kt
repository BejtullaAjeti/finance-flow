package com.example.financeflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.locale.rememberDateFormat
import java.time.LocalDate

/** A tap-to-open single-date field — a read-only [GlassTextField] (same border/radius/colors as
 * every other input) plus [CalendarDialog], not a one-off styled row. */
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = rememberDateFormat("MMM d, yyyy")

    Box(modifier = modifier.fillMaxWidth()) {
        GlassTextField(
            value = date.format(dateFormat),
            onValueChange = {},
            label = label,
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Rounded.DateRange, contentDescription = stringResource(R.string.date_field_change_content_description, label))
            },
            modifier = Modifier.fillMaxWidth()
        )
        // GlassTextField is readOnly, not disabled, so it would otherwise take focus/show a
        // cursor on tap instead of opening the calendar — this transparent overlay intercepts
        // the tap before it reaches the text field.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showPicker = true }
        )
    }

    if (showPicker) {
        CalendarDialog(
            initialDate = date,
            onConfirm = { onDateChange(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}
