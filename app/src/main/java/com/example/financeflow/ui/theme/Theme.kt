package com.example.financeflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Shared corner-radius scale — medium matches GlassCard's own 24dp default, so every themed
// surface (text fields, buttons, dialogs) reads as part of the same rounded language.
private val FinanceFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val FinanceFlowDarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Income,
    onSecondary = OnAccent,
    secondaryContainer = IncomeContainer,
    tertiary = Expense,
    onTertiary = OnAccent,
    tertiaryContainer = ExpenseContainer,
    background = Backdrop,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    error = Expense,
    onError = OnAccent
)

// ponytail: a real light theme is v2 ("Dark mode/theming" is a nice-to-have in CLAUDE.md); this
// only exists so MaterialTheme has a non-crashing fallback if isSystemInDarkTheme() ever replaces
// the fixed `darkTheme = true` default below.
private val FinanceFlowLightScheme = lightColorScheme(
    primary = Accent,
    secondary = Income,
    tertiary = Expense
)

@Composable
fun FinanceFlowTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FinanceFlowDarkScheme else FinanceFlowLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FinanceFlowShapes,
        content = content
    )
}
