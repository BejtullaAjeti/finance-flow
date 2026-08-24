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
    // button, the bottom-nav indicator, the selected calendar day). Tracks the primary slot in
    // both modes: every one of those call sites pairs this fill with `onPrimary` as its content
    // color, so the two must stay in step. (Dark mode previously used AccentDark here; once
    // onPrimary became a light off-white, off-white-on-cream measured 1.32:1.) Distinct from
    // [accent] below, which is the featured-card fill and is not paired with onPrimary.
    val selectedFill: Color,
    // Border color for controls whose border carries the affordance (unselected filter chips,
    // text-field and date-range outlines, the chart's axis line). M3's ColorScheme has no slot
    // for it — `outlineVariant` is defined as *weaker* than outline, the opposite of what this
    // is — so it lives here. See BorderStrongLight/Dark in Color.kt for the contrast targets.
    val borderStrong: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val disabledFill: Color,
    val disabledContent: Color,
    val onConfirm: Color
)

val LightExtendedColors = ExtendedColors(
    // Not SurfaceLight: that role now equals `background`, which would leave featured cards
    // invisible. SurfaceVariantLight preserves the original intent — spec §3 has the light
    // featured card share the ordinary card fill; only dark mode gives it a distinct tone.
    accent = SurfaceVariantLight,
    selectedFill = PrimaryLight,
    borderStrong = BorderStrongLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarning = OnWarningLight,
    disabledFill = DisabledFillLight,
    disabledContent = DisabledContentLight,
    onConfirm = Color.White
)

val DarkExtendedColors = ExtendedColors(
    accent = AccentDark,
    selectedFill = PrimaryDark,
    borderStrong = BorderStrongDark,
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
