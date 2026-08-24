package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────────
// Primary is mode-dependent as of this pass. It used to be a single fixed value shared by both
// schemes ("identical in both modes per spec §3"); the new palette gives light and dark their own
// stop, so the old single `Primary` token is now `PrimaryLight` / `PrimaryDark`.
val PrimaryLight = Color(0xFF04342C)
val PrimaryDark = Color(0xFF085041)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryDark = Color(0xFFF1EFE8)

// Near-black ink for content drawn on the *pale* semantic fills — dark mode's income/expense and
// error hues are light, so they need dark content. This was `OnPrimaryColor`; it no longer feeds
// the onPrimary slot (that's OnPrimaryLight/OnPrimaryDark above), only onSecondary/onTertiary/
// onError, none of which the new table specifies.
val OnAccentInk = Color(0xFF1B140A)

// ── Background / Surface ─────────────────────────────────────────────────────
// Background and Surface carry the SAME value per mode in the new palette. They stay separate
// tokens so the roles can diverge again without touching call sites — but see the note on
// surfaceVariant below: with these values, surface no longer separates from background.
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF2C2C2A)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2C2C2A)

// Secondary cards — the only neutral fill that reads as distinct from Background now.
val SurfaceVariantLight = Color(0xFFF1EFE8)
val SurfaceVariantDark = Color(0xFF444441)

// Accent — featured cards only (Home summary, budget cards). Dark mode keeps its own warm tone
// so featured cards still stand out from ordinary surfaceVariant rows; light mode's accent is
// SurfaceVariantLight (see ExtendedColors.kt), matching spec §3, where the light featured card
// shares the ordinary card fill.
val AccentDark = Color(0xFFDFD0B8)

// ── Text ─────────────────────────────────────────────────────────────────────
val OnBackgroundLight = Color(0xFF2C2C2A)
val OnBackgroundDark = Color(0xFFF1EFE8)
val OnSurfaceMutedLight = Color(0xFF5F5E5A)
val OnSurfaceMutedDark = Color(0xFFB4B2A9)

// ── Borders / dividers ───────────────────────────────────────────────────────
// New tokens. `outline` previously aliased OnSurfaceMuted*, which made every border as dark as
// secondary text; the new palette gives dividers their own, much lighter stop.
val OutlineLight = Color(0xFFD3D1C7)
val OutlineDark = Color(0xFF444441)

// One stop stronger than Outline, for borders that are a control's *only* affordance — an
// unselected filter chip has no fill, so its border is the whole control. Outline is far too
// faint for that (1.53:1 light / 1.43:1 dark). These are the lightest/darkest stops in the same
// warm-grey family that still clear the 3:1 WCAG 1.4.11 minimum against BOTH `background` and
// `surfaceVariant`, so a bordered control reads correctly wherever it is placed. Deliberately
// not pushed further: onSurfaceVariant (6.5:1) would draw a border at secondary-text weight.
// Dividers and chart guidelines stay on Outline — they are decorative, not affordances.
val BorderStrongLight = Color(0xFF8C8A80)
val BorderStrongDark = Color(0xFF918F85)

// ── Money semantics ──────────────────────────────────────────────────────────
val IncomeLight = Color(0xFF0F6E56)
val IncomeDark = Color(0xFF5DCAA5)
// Container tints were not supplied in the new table — carried over unchanged. Both still sit in
// the same hue family as their re-tuned parents.
val IncomeContainerLight = Color(0xFFDCEFE2)
val IncomeContainerDark = Color(0xFF20302A)

val ExpenseLight = Color(0xFF993C1D)
val ExpenseDark = Color(0xFFF0997B)
val ExpenseContainerLight = Color(0xFFF5DCD0)
val ExpenseContainerDark = Color(0xFF352420)

// ── Roles NOT in the new table — unchanged below this line ───────────────────

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

// ── Category accents ─────────────────────────────────────────────────────────
/**
 * A category's paired container fill and the content color that sits on it. Same hue in both
 * modes, swapped stops: the light-mode container becomes the dark-mode content and vice versa.
 *
 * NOT WIRED UP YET. Categories currently persist a single hex string (`Category.color`) that
 * [com.example.financeflow.ui.components.CategoryStyle] parses mode-blind, with content color
 * derived from luminance — that model cannot express a per-mode pair. See the handoff notes.
 */
data class CategoryAccent(val container: Color, val content: Color)

val CategoryAccentsLight = listOf(
    CategoryAccent(container = Color(0xFFF5C4B3), content = Color(0xFF712B13)), // A
    CategoryAccent(container = Color(0xFFB5D4F4), content = Color(0xFF0C447C)), // B
    CategoryAccent(container = Color(0xFFCECBF6), content = Color(0xFF3C3489)), // C
    CategoryAccent(container = Color(0xFFFAC775), content = Color(0xFF633806))  // D
)

val CategoryAccentsDark = listOf(
    CategoryAccent(container = Color(0xFF712B13), content = Color(0xFFF0997B)), // A
    CategoryAccent(container = Color(0xFF0C447C), content = Color(0xFF85B7EB)), // B
    CategoryAccent(container = Color(0xFF3C3489), content = Color(0xFFAFA9EC)), // C
    CategoryAccent(container = Color(0xFF633806), content = Color(0xFFEF9F27))  // D
)
