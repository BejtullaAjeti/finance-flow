package com.example.financeflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Which flat surface color a component reads by default. No longer an elevation/glass tier —
 * kept as a lightweight enum so most call sites don't need per-instance color plumbing. */
enum class GlassTier { Row, Card, Interactive, Overlay }

data class GlassTierSpec(val fill: Color)

@Composable
fun GlassTier.spec(): GlassTierSpec = when (this) {
    GlassTier.Row -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
    GlassTier.Card -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
    GlassTier.Interactive -> GlassTierSpec(fill = MaterialTheme.colorScheme.primary)
    GlassTier.Overlay -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
}
