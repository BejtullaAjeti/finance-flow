package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing

/** Flat list row (transactions, categories, recurring rules) — no border. Fills with
 * `surfaceVariant`; see [com.example.financeflow.ui.theme.spec] for why not `surface`. */
@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // Radius.medium so rows share the 16dp card language (matches GlassCard).
    shape: Shape = RoundedCornerShape(Radius.medium),
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (onClick != null) {
        base = base
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
    }
    Row(modifier = base.padding(contentPadding), content = content)
}
