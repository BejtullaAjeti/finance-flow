package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassBorderBottom
import com.example.financeflow.ui.theme.GlassBorderTop
import com.example.financeflow.ui.theme.GlassFill
import com.example.financeflow.ui.theme.GlassGlow
import com.example.financeflow.ui.theme.GlassShadow

/**
 * Shared floating action button styled to match [GlassCard] — same fill/border/shadow tokens —
 * instead of M3's flat default FAB. Larger than the M3 default (64dp vs 56dp) for an easier
 * thumb target, and used everywhere the app needs a primary "add" action (Home, Transactions,
 * Recurring, Categories) so the affordance is visually consistent across screens.
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(elevation = 16.dp, shape = shape, ambientColor = GlassShadow, spotColor = GlassShadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(GlassGlow, GlassFill)))
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)), shape)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                this.role = androidx.compose.ui.semantics.Role.Button
            },
        contentAlignment = Alignment.Center,
        content = { icon() }
    )
}
