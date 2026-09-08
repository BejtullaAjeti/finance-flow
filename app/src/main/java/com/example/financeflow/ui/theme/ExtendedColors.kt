package com.example.financeflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mode-dependent color roles the minimalist design system needs that M3's ColorScheme has no
 * slot for — the budget warning tier and the confirm button's disabled/enabled text pairing.
 * Structural UI color — Background, Surface, Accent — lives entirely in
 * [MaterialTheme.colorScheme] (background/surface/primary); it does not belong here. Provided by
 * [FinanceFlowTheme] alongside MaterialTheme's own colorScheme; read the same way via
 * [MaterialTheme.extendedColors].
 */
data class ExtendedColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val disabledFill: Color,
    val disabledContent: Color,
    val onConfirm: Color
)

val LightExtendedColors = ExtendedColors(
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarning = OnWarningLight,
    disabledFill = DisabledFillLight,
    disabledContent = DisabledContentLight,
    onConfirm = Color.White
)

val DarkExtendedColors = ExtendedColors(
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    onWarning = OnWarningDark,
    disabledFill = DisabledFillDark,
    disabledContent = DisabledContentDark,
    onConfirm = OnConfirmDark
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
