package com.example.financeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.selectionRing(
    selected: Boolean,
    shape: Shape,
    color: Color,
    width: Dp = 2.dp
): Modifier = composed {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) color else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "selectionRing"
    )
    border(width, animatedColor, shape)
}
