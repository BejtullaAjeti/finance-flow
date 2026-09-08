package com.example.financeflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Shared corner-radius scale, tightened for the minimalist system (spec §5) — every themed
// surface (text fields, buttons, dialogs) reads as part of the same crisp rounded language.
private val FinanceFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.extraSmall),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.large)
)

private val FinanceFlowLightScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentColor,
    secondary = IncomeLight,
    onSecondary = OnAccentColor,
    secondaryContainer = IncomeContainerLight,
    tertiary = ExpenseLight,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    // Unset, this falls back to M3's baseline outline swatch — a generic purple-grey never
    // chosen for this palette — so every bordered input (GlassTextField, GlassFilterChip) was
    // quietly drawing a border color outside the design system.
    outline = OnSurfaceMutedLight,
    error = ExpenseLight,
    onError = Color.White
)

// `primary` is this scheme's Accent role — it must be AccentDark, not AccentLight. Reusing the
// light hue here was the dark-mode bug: the FAB and every Accent-filled button rendered in the
// light palette's purple instead of the dark palette's tan, because `primary` was a single
// `Primary` constant shared by both schemes instead of switching per mode like every other role.
private val FinanceFlowDarkScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentColor,
    secondary = IncomeDark,
    onSecondary = OnAccentColor,
    secondaryContainer = IncomeContainerDark,
    tertiary = ExpenseDark,
    onTertiary = OnAccentColor,
    tertiaryContainer = ExpenseContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OnSurfaceMutedDark,
    error = ExpenseDark,
    onError = OnAccentColor
)

@Composable
fun FinanceFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FinanceFlowDarkScheme else FinanceFlowLightScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FinanceFlowShapes,
            content = content
        )
    }
}
