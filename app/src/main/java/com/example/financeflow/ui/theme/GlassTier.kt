package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTier { Row, Card, Interactive, Overlay }

data class GlassTierSpec(
    val fill: Color,
    val borderBrush: Brush,
    val glowBlur: Dp?,
    val glowColor: Color?,
    val shadowElevation: Dp,
    val shadowColor: Color
)

fun GlassTier.spec(): GlassTierSpec = when (this) {
    GlassTier.Row -> GlassTierSpec(
        fill = Color(0x14FFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))),
        glowBlur = null,
        glowColor = null,
        shadowElevation = 0.dp,
        shadowColor = Color.Transparent
    )
    GlassTier.Card -> GlassTierSpec(
        fill = GlassFill,
        borderBrush = Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)),
        glowBlur = 24.dp,
        glowColor = GlassGlow,
        shadowElevation = 20.dp,
        shadowColor = GlassShadow
    )
    GlassTier.Interactive -> GlassTierSpec(
        fill = Color(0x2EFFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x70FFFFFF), GlassBorderBottom)),
        glowBlur = 24.dp,
        glowColor = GlassGlow,
        shadowElevation = 20.dp,
        shadowColor = GlassShadow
    )
    GlassTier.Overlay -> GlassTierSpec(
        fill = Color(0x30FFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x14FFFFFF))),
        glowBlur = 32.dp,
        glowColor = Color(0x40FFFFFF),
        shadowElevation = 28.dp,
        shadowColor = Color(0x85000000)
    )
}
