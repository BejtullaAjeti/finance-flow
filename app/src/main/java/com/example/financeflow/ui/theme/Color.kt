package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Color

// The exactly-3 structural color roles per theme. Every component must pull its fill from one of
// these — via MaterialTheme.colorScheme.background/surface/primary — never a hardcoded hex:
//   Background — base screen color
//   Surface    — cards, list rows, inputs, dialogs: the "element" color
//   Accent     — buttons, selected states, active nav indicator, FAB — interactive/selected only,
//                never a full card/section background behind body text. That misuse (Accent as
//                the "This Month"/budget card fill) is exactly what made dark mode's near-white
//                body text unreadable against the pale tan accent — those cards use Surface now.
// Previously Accent was two conflicting values: a `Primary` hardcoded to the light hue in BOTH
// color schemes (so the FAB/buttons stayed light-purple in dark mode), and a separate
// `extendedColors.selectedFill` that correctly switched per mode. There is now exactly one
// per-mode Accent — colorScheme.primary — and every selected/active call site reads that.
val BackgroundLight = Color(0xFFFFF4F4)
val BackgroundDark = Color(0xFF222831)
val SurfaceLight = Color(0xFFFFD6E0)
val SurfaceDark = Color(0xFF393E46)
val AccentLight = Color(0xFFBDB2FF)
val AccentDark = Color(0xFFDFD0B8)
// Both accent tones are pale, so both modes want the same near-black text/icon color on top.
val OnAccentColor = Color(0xFF1B140A)

// Text
val OnBackgroundLight = Color(0xFF1A1A1A)
val OnBackgroundDark = Color(0xFFEDE8DD)
// Spec §3 gives "mid grey" for both modes without exact hex; tuned separately per background so
// each still clears ~4.5:1 against its own Background/Surface (light needs a darker grey than
// dark does to hit the same contrast ratio against a near-white vs. a near-black backdrop).
val OnSurfaceMutedLight = Color(0xFF6E6E73)
val OnSurfaceMutedDark = Color(0xFF9AA2B1)

// Money semantics — light-mode hues tuned for WCAG AA against the pale backgrounds; dark-mode
// hues brightened off the original pre-redesign values, which read too flat against SurfaceDark
// (#7FB88F was only 4.7:1, #D9825F only 3.75:1 — under AA's 4.5:1 for normal-weight text). These
// clear ~6.3:1 / ~5.3:1 against SurfaceDark while keeping the same green/orange hue family.
val IncomeLight = Color(0xFF2F6B4F)
val IncomeDark = Color(0xFF7ED99A)
// Spec §3 says container colors are "pale tint of each hue" for light mode, no exact hex given —
// these are pale desaturated tints of Income/Expense/Warning's light-mode hues.
val IncomeContainerLight = Color(0xFFDCEFE2)
val IncomeContainerDark = Color(0xFF20302A)

val ExpenseLight = Color(0xFF9C4419)
val ExpenseDark = Color(0xFFEDA582)
val ExpenseContainerLight = Color(0xFFF5DCD0)
val ExpenseContainerDark = Color(0xFF352420)

val WarningLight = Color(0xFF855708)
val WarningDark = Color(0xFFD9A441)
val WarningContainerLight = Color(0xFFF5E6C8)
val WarningContainerDark = Color(0xFF362B18)
// Text/icon drawn on a solid Warning fill. WarningLight (#855708) is a dark mustard-brown, so it
// needs light text; WarningDark (#D9A441) is a pale gold and already had a near-black OnWarning.
val OnWarningLight = Color(0xFFFFF4F4)
val OnWarningDark = Color(0xFF1B140A)

// ConfirmButton's disabled state, exact per spec §3
val DisabledFillLight = Color(0xFFD9D2D2)
val DisabledContentLight = Color(0xFF8A8080)
val DisabledFillDark = Color(0xFF4A4F57)
val DisabledContentDark = Color(0xFF9AA0A8)

// ConfirmButton's enabled-state text/icon color — dark mode's Income fill is pale, needs near-
// black text; light mode's Income fill is a deep forest green, needs white (defined inline in
// ExtendedColors.kt as Color.White since it's not otherwise reused).
val OnConfirmDark = Color(0xFF1B140A)
