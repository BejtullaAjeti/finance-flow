package com.example.financeflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.theme.extendedColors

/**
 * Flat drop-in for M3's FilterChip — unselected is border-only (no fill), selected is a solid
 * Accent fill (colorScheme.primary, not a generic M3 highlight) with no border, per the
 * minimalist policy's chip rules (spec §6). Uses [Radius.medium], the one shared corner radius
 * the whole app uses — it stays well under half of FilterChip's fixed 32dp minimum height, so
 * this still reads as a rounded rect (not a pill) like every other Radius.medium surface.
 */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        // M3's own chip padding reads as cramped against the border — this wraps the caller's
        // label with extra breathing room rather than fighting FilterChip's internal padding.
        label = {
            Box(modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs), contentAlignment = Alignment.Center) {
                label()
            }
        },
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(Radius.medium),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            // Unselected chips have no fill, so this border is the entire control —
            // borderStrong, not outline. selectedBorderColor is inert (0dp width below).
            borderColor = MaterialTheme.extendedColors.borderStrong,
            selectedBorderColor = MaterialTheme.colorScheme.outline,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}
