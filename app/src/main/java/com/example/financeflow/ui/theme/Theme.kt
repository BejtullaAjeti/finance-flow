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
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    secondary = IncomeLight,
    onSecondary = OnAccentInk,
    secondaryContainer = IncomeContainerLight,
    tertiary = ExpenseLight,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    // Was aliased to SurfaceLight; the new palette gives secondary cards their own stop, so this
    // is now the only neutral fill that separates from `background`.
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    // Was aliased to OnSurfaceMutedLight, which drew every border as dark as secondary text.
    // The new palette gives dividers a dedicated, far lighter stop.
    outline = OutlineLight,
    error = ExpenseLight,
    onError = Color.White
)

private val FinanceFlowDarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    secondary = IncomeDark,
    onSecondary = OnAccentInk,
    secondaryContainer = IncomeContainerDark,
    tertiary = ExpenseDark,
    onTertiary = OnAccentInk,
    tertiaryContainer = ExpenseContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OutlineDark,
    error = ExpenseDark,
    onError = OnAccentInk
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
