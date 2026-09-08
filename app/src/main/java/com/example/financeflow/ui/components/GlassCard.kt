package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.spec

/**
 * Flat solid-color surface for cards, dialogs, and other elevated content — no border, no
 * shadow, no blur. [tier] picks the fill (Row/Card/Overlay all resolve to Surface, Interactive to
 * Accent) — no per-call-site override, so a card can never end up filled with Accent behind body
 * text (that mismatch — near-white text on a pale Accent card — was the dark-mode contrast bug on
 * "This Month"/budget cards; both are plain Surface-tier cards now like everywhere else).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    cornerRadius: Dp = Radius.medium,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = tier.spec()
    Column(
        modifier = modifier
            .clip(shape)
            .background(spec.fill)
            .padding(contentPadding),
        content = content
    )
}
