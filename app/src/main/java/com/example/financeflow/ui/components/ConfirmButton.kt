package com.example.financeflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.ui.theme.extendedColors

/**
 * Shared primary/affirmative action for every dialog and form — a true pill regardless of
 * height, per the minimalist design system's confirm-button pattern (spec §7). Label is always
 * "Confirm", not "Save"/"Add"/"Import" — one predictable "ready to submit" signal app-wide,
 * deliberate per the spec rather than an oversight.
 */
@Composable
fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.extendedColors.onConfirm,
            disabledContainerColor = MaterialTheme.extendedColors.disabledFill,
            disabledContentColor = MaterialTheme.extendedColors.disabledContent
        )
    ) {
        if (enabled) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        }
        Text(stringResource(R.string.action_confirm))
    }
}
