package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.theme.spec

/**
 * Row-tier glass wrapper for dense list rows (transactions, categories, recurring rules) —
 * the most restrained tier in the elevation scale, tuned low so financial figures stay scannable.
 */
@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.extraSmall),
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    content: @Composable RowScope.() -> Unit
) {
    val spec = GlassTier.Row.spec()
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(shape)
        .background(spec.fill)
        .border(1.dp, spec.borderBrush, shape)
    if (onClick != null) {
        base = base
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
    }
    Row(modifier = base.padding(contentPadding), content = content)
}
