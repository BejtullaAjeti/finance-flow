package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.spec

/**
 * Shared floating action button styled to match [GlassCard] at [GlassTier.Interactive] — same
 * fill/border/shadow/glow tokens — instead of M3's flat default FAB. Larger than the M3 default
 * (64dp vs 56dp) for an easier thumb target, and used everywhere the app needs a primary "add"
 * action (Home, Transactions, Recurring, Categories) so the affordance is visually consistent
 * across screens.
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val spec = GlassTier.Interactive.spec()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(elevation = spec.shadowElevation, shape = shape, ambientColor = spec.shadowColor, spotColor = spec.shadowColor)
            .clip(shape)
            .background(spec.fill)
            .border(1.dp, spec.borderBrush, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
            .semantics {
                this.contentDescription = contentDescription
                this.role = androidx.compose.ui.semantics.Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // ponytail: same glow-patch technique as GlassCard (blur only renders API 31+, inert
        // below that — fill/border/shadow alone still read as glass).
        if (spec.glowBlur != null && spec.glowColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(64.dp)
                    .blur(spec.glowBlur)
                    .background(spec.glowColor, shape)
            )
        }
        icon()
    }
}
