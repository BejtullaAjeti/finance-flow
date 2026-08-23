package com.example.financeflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/**
 * Shared secondary/dismiss action for every dialog and form — same pill shape and size as
 * [ConfirmButton], border-only (no fill) per the minimalist policy's "background XOR border"
 * component rule, so it reads as a matched pair instead of one prominent button next to an
 * easy-to-miss text link.
 */
@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(stringResource(R.string.action_cancel))
    }
}

/**
 * Pill-shaped "add category" action, styled like [ConfirmButton]/[CancelButton] (same shape,
 * icon + text) instead of a plain unselected filter chip — the accent fill
 * (extendedColors.selectedFill) marks it as a distinct action, not another selectable option in
 * the chip row it sits in.
 */
@Composable
fun AddCategoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.extendedColors.selectedFill,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(stringResource(R.string.categories_add_new_chip))
    }
}
