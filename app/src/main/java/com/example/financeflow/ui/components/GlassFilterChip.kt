package com.example.financeflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassBorderTop
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.OnBackground
import com.example.financeflow.ui.theme.spec

/**
 * Glass-styled drop-in for M3's FilterChip — same API, but both the unselected (transparent by
 * M3 default) and selected (solid secondaryContainer by M3 default) states use the app's glass
 * fill/border tokens instead, so chips read as the same "floating glass" material as everything
 * else rather than a flat filled pill when selected.
 */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = GlassTier.Interactive.spec()
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = spec.fill,
            labelColor = OnBackground,
            selectedContainerColor = spec.glowColor ?: spec.fill,
            selectedLabelColor = OnBackground
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = GlassBorderTop,
            selectedBorderColor = GlassBorderTop,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}
