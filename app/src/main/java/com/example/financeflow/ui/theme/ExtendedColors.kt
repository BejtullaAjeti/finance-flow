package com.example.financeflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mode-dependent color roles the minimalist design system needs that M3's ColorScheme has no
 * slot for — the featured-card accent fill, the budget warning tier, and the confirm button's
 * disabled/enabled text pairing. Provided by [FinanceFlowTheme] alongside MaterialTheme's own
 * colorScheme; read the same way via [MaterialTheme.extendedColors].
 */
data class ExtendedColors(
    val accent: Color,
    // Selected/active fill for interactive surfaces (filter chips, the inline "add category"
    // button) — the app's real per-mode accent (#BDB2FF light / #DFD0B8 dark), distinct from
    // [accent] above (which is the featured-card fill) and from Primary (fixed across modes,
    // used by buttons/FAB/segmented controls).
    val selectedFill: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val disabledFill: Color,
    val disabledContent: Color,
    val onConfirm: Color
)

val LightExtendedColors = ExtendedColors(
    accent = SurfaceLight,
    selectedFill = Primary,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarning = OnWarningLight,
    disabledFill = DisabledFillLight,
    disabledContent = DisabledContentLight,
    onConfirm = Color.White
)

val DarkExtendedColors = ExtendedColors(
    accent = AccentDark,
    selectedFill = AccentDark,
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
