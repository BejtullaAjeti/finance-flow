package com.example.financeflow.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.theme.Radius
import java.time.LocalDate

/** A tap-to-open single-date field: a bordered label+value row plus [CalendarDialog]. */
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = rememberDateFormat("MMM d, yyyy")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Radius.extraSmall))
            .clickable { showPicker = true }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = date.format(dateFormat), style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Rounded.DateRange, contentDescription = stringResource(R.string.date_field_change_content_description, label))
    }

    if (showPicker) {
        CalendarDialog(
            initialDate = date,
            onConfirm = { onDateChange(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}
