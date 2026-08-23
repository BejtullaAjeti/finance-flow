package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.spec

/**
 * Flat solid-color surface for cards, dialogs, and other elevated content — no border, no
 * shadow, no blur. [tier] picks the default fill (Row/Card/Overlay all resolve to Surface,
 * Interactive to Primary); pass [containerColor] to override, used by the two "featured card"
 * call sites (Home summary, budget cards) that use Accent instead.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    containerColor: Color? = null,
    cornerRadius: Dp = Radius.medium,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = tier.spec()
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor ?: spec.fill)
            .padding(contentPadding),
        content = content
    )
}
