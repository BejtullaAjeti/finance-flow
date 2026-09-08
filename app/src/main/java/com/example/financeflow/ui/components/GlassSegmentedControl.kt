package com.example.financeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing

/** Flat Surface-background segmented control — selected segment is a solid Accent fill. Used
 * for every segmented control in the app (type toggles, tabs, period pickers, settings rows). */
@Composable
fun <T> GlassSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector?)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.small))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.xs)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val segmentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(180),
                label = "segmentBackground"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "segmentText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.extraSmall))
                    .background(segmentColor)
                    .clickable { onSelect(option) }
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                val optionIcon = icon?.invoke(option)
                if (optionIcon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(optionIcon, contentDescription = null, tint = textColor, modifier = Modifier.padding(end = Spacing.xs))
                        Text(text = label(option), color = textColor, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(text = label(option), color = textColor, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
