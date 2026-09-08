package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The one shared list-row shell — flat Surface fill via [GlassRow] (no border, no shadow), a
 * category-colored 32dp icon swatch, a title (with an optional inline type-indicator icon), an
 * optional multi-line subtitle, a trailing slot (amount, switch, amount+percentage...), and an
 * optional block of extra content below the row (Budgets' progress bar). Every list in the app —
 * Reports' category breakdown, Home/Transactions rows, Categories, Recurring, Budgets — builds on
 * this instead of each hand-rolling its own row/card shape, padding, and radius.
 */
@Composable
fun ListRow(
    icon: ImageVector,
    swatchColor: Color,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    titleTrailingIcon: ImageVector? = null,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    GlassRow(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(swatchColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (titleTrailingIcon != null) {
                            Spacer(Modifier.width(6.dp))
                            TypeIndicatorIcon(titleTrailingIcon)
                        }
                    }
                    subtitle?.invoke(this)
                }
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (extra != null) {
                Spacer(Modifier.height(12.dp))
                extra()
            }
        }
    }
}
