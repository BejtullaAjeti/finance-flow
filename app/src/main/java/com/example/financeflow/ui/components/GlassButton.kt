package com.example.financeflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassBorderBottom
import com.example.financeflow.ui.theme.GlassBorderTop
import com.example.financeflow.ui.theme.GlassFill
import com.example.financeflow.ui.theme.OnBackground

/**
 * Glass-styled drop-in for M3's Button — same API, but a translucent fill and gradient border
 * instead of a solid primary-color fill, matching the rest of the app's glass surfaces.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = GlassFill, contentColor = OnBackground),
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom))),
        content = content
    )
}
