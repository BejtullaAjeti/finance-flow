package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.spec

/**
 * Frosted-glass surface for accent content — dashboard summary, budget progress, bottom nav —
 * per the Design & Styling section of CLAUDE.md. [tier] controls how prominent the glass reads;
 * dense lists should use [GlassRow] (Row tier) instead of this at [GlassTier.Card].
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = tier.spec()
    Box(
        modifier = modifier
            .shadow(elevation = spec.shadowElevation, shape = shape, ambientColor = spec.shadowColor, spotColor = spec.shadowColor)
            .clip(shape)
            .background(spec.fill)
            .border(1.dp, spec.borderBrush, shape)
    ) {
        // ponytail: RenderEffect blur only renders on API 31+ (minSdk is 26); below that this
        // glow patch is simply inert, and the card still reads as glass via the translucent
        // fill + gradient border + shadow on their own.
        if (spec.glowBlur != null && spec.glowColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(contentPadding * 2)
                    .blur(spec.glowBlur)
                    .background(spec.glowColor, shape)
            )
        }
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
