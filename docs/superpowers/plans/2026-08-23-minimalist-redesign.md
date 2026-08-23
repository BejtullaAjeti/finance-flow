# Minimalist Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the app's dark-only glassmorphism design system (translucent fills, gradient
borders, blurred glow patches, drop shadows) with the flat, minimalist, light/dark-aware system
specced in `docs/superpowers/specs/2026-08-23-minimalist-redesign-design.md` — zero remaining
transparency/gradient/blur effects anywhere in the app.

**Architecture:** Presentation-layer only — no ViewModel/Room/DAO/query changes. Two layers:
(1) shared tokens and components (`ui/theme/*`, `ui/components/Glass*`, two new components
`ConfirmButton`/`CalendarDialog`), (2) sequential per-screen application, in the order the spec's
§9 names: Home → Add/Edit Transaction → Transactions → Categories → Budgets → Reports →
Recurring Rules → Settings, then bottom-nav polish and final cleanup.

**Important scoping note:** despite three prior commits with "Redesign X screen" messages
(`4d7df4d`..`c6c3c32`), no code in this repo currently implements the minimalist spec — every
screen and every `Glass*` component is still on the original dark-only glass system. This plan
starts from that actual state, not from an assumed partial conversion.

**Tech Stack:** Jetpack Compose, Material3, `java.time` (for the new calendar dialog — no new
dependency), existing Phosphor Icons dependency (`com.adamglin:phosphor-icons`, already used
app-wide).

**Spec:** `docs/superpowers/specs/2026-08-23-minimalist-redesign-design.md`

## Global Constraints

- No new Gradle dependencies (spec §2).
- No ViewModel/Room/DAO/query logic changes (spec §2) — presentation only.
- No manual light/dark toggle — `FinanceFlowTheme` follows `isSystemInDarkTheme()` (spec §2).
- No nav structure changes (spec §2).
- **Keep existing `Glass*` component names** (`GlassCard`, `GlassRow`, `GlassButton`,
  `GlassTextField`, `GlassSegmentedControl`, `GlassDialog`, `GlassFilterChip`, `GlassFab`,
  `GlassTier`) — this is a deliberate scoping call, not an oversight: renaming them would touch
  every call site across ~15 files for zero behavioral benefit. Only their *internals* become
  flat/solid. `GlassAlpha` is the one object that becomes fully dead and gets deleted (Task 21)
  once every `.copy(alpha = GlassAlpha.X)` call site is converted to a solid color.
- Every user-facing string continues to go through `strings.xml` / `values-en/strings.xml`.
- Every task must leave `gradlew compileDebugKotlin` succeeding. Where noted, also run
  `gradlew testDebugUnitTest` (existing suite: `ConvertersTest`, `RecurringRuleGenerationTest`,
  `ReportPeriodRangeTest`, `BackupSerializerTest`, `BudgetViewModelTest`, `CategoryViewModelTest`,
  `CategoryViewModelTest`) and confirm it's still green — none of this pass's changes touch logic
  any of them exercise, so a failure means a mistake, not an expected update.
- **Acceptance bar for the whole pass:** after Task 22, `grep -rn "Brush\.verticalGradient\|Brush\.horizontalGradient\|\.blur(\|\.copy(alpha" app/src/main/java/com/example/financeflow/ui` must return **zero** matches (aside from the one documented, deliberate exception noted in Task 12 Step 4 — the stock M3 `DateRangePicker`, which the spec does not define a flat replacement for).
- No shadows anywhere (spec §6, confirmed explicitly) — every `.shadow(...)` call in `ui/components/Glass*.kt` is removed, not tuned down.
- This pass is UI/theming, not business logic — verification per task is a compile check plus a specific manual visual check to perform in the running app (light AND dark mode, since both are now real distinct themes), not a fabricated unit test. Exceptions: `CalendarDialog`'s month-grid date math (Task 4) and `FinanceFlowDestination.matchesCurrentRoute` (Task 19) are pure functions and get a real assertion-based check.

---

## Task 1: Color tokens — light/dark palettes, `ExtendedColors`, `Theme.kt`, `Radius.kt`

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/financeflow/ui/theme/ExtendedColors.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Radius.kt`

**Interfaces:**
- Produces: `MaterialTheme.extendedColors: ExtendedColors` (accessed via
  `MaterialTheme.extendedColors.accent/.warning/.warningContainer/.onWarning/.disabledFill/
  .disabledContent/.onConfirm`), available anywhere inside `FinanceFlowTheme`. `Radius.extraSmall/
  small/medium/large` (values changed, names unchanged). `MaterialTheme.colorScheme.secondary` =
  Income, `.secondaryContainer` = IncomeContainer, `.tertiary` = Expense, `.tertiaryContainer` =
  ExpenseContainer, `.primary` = Primary/OnPrimary, `.surface`/`.background`/`.onSurfaceVariant`
  per mode — all now genuinely switch with system light/dark, unlike the current dark-only vals.
- Judgment calls made where the spec table didn't give an exact hex (documented inline in the
  code as comments, matching how the spec itself explains its own color choices): `OnSurfaceMuted`
  light-mode value, `IncomeContainer`/`ExpenseContainer`/`WarningContainer` light-mode "pale tint"
  values, `OnWarning` light-mode value.

- [ ] **Step 1: Rewrite `Color.kt`**

Replace the entire file contents with:

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Color

// Background / Surface
val BackgroundLight = Color(0xFFFFF4F4)
val BackgroundDark = Color(0xFF222831)
val SurfaceLight = Color(0xFFFFD6E0)
val SurfaceDark = Color(0xFF393E46)

// Accent — featured cards only (Home summary, budget cards). Same as Surface in light mode per
// spec §3; a distinct warm tone in dark mode so featured cards still stand out from ordinary rows.
val AccentDark = Color(0xFFDFD0B8)

// Primary — buttons/FAB/selected chip/selected segment, identical in both modes per spec §3
val Primary = Color(0xFFBDB2FF)
val OnPrimaryColor = Color(0xFF1B140A)

// Text
val OnBackgroundLight = Color(0xFF1A1A1A)
val OnBackgroundDark = Color(0xFFEDE8DD)
// Spec §3 gives "mid grey" for both modes without exact hex; tuned separately per background so
// each still clears ~4.5:1 against its own Background/Surface (light needs a darker grey than
// dark does to hit the same contrast ratio against a near-white vs. a near-black backdrop).
val OnSurfaceMutedLight = Color(0xFF6E6E73)
val OnSurfaceMutedDark = Color(0xFF9AA2B1)

// Money semantics — light-mode hues re-tuned for WCAG AA against the new pale backgrounds (see
// spec §3's contrast math); dark-mode values are the pre-existing ones, unchanged.
val IncomeLight = Color(0xFF2F6B4F)
val IncomeDark = Color(0xFF7FB88F)
// Spec §3 says container colors are "pale tint of each hue" for light mode, no exact hex given —
// these are pale desaturated tints of Income/Expense/Warning's light-mode hues.
val IncomeContainerLight = Color(0xFFDCEFE2)
val IncomeContainerDark = Color(0xFF20302A)

val ExpenseLight = Color(0xFF9C4419)
val ExpenseDark = Color(0xFFD9825F)
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
```

- [ ] **Step 2: Create `ExtendedColors.kt`**

```kotlin
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
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val disabledFill: Color,
    val disabledContent: Color,
    val onConfirm: Color
)

val LightExtendedColors = ExtendedColors(
    accent = SurfaceLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarning = OnWarningLight,
    disabledFill = DisabledFillLight,
    disabledContent = DisabledContentLight,
    onConfirm = Color.White
)

val DarkExtendedColors = ExtendedColors(
    accent = AccentDark,
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
```

- [ ] **Step 3: Rewrite `Radius.kt`**

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.ui.unit.dp

object Radius {
    val extraSmall = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
}
```

(Drops `extraLarge` — it duplicated `large`'s value and has zero call sites; confirmed via
`grep -rn "Radius.extraLarge" app/src/main/java`.)

- [ ] **Step 4: Rewrite `Theme.kt`**

```kotlin
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
import androidx.compose.ui.unit.dp

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
    primary = Primary,
    onPrimary = OnPrimaryColor,
    secondary = IncomeLight,
    onSecondary = OnPrimaryColor,
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
    error = ExpenseLight,
    onError = Color.White
)

private val FinanceFlowDarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryColor,
    secondary = IncomeDark,
    onSecondary = OnPrimaryColor,
    secondaryContainer = IncomeContainerDark,
    tertiary = ExpenseDark,
    onTertiary = OnPrimaryColor,
    tertiaryContainer = ExpenseContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    error = ExpenseDark,
    onError = OnPrimaryColor
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
```

Note: `secondary`/`tertiary`/`error` roles aren't read directly by any app code today (confirmed
via `grep -rn "colorScheme\.\(secondary\|tertiary\|error\)" app/src/main/java/com/example/
financeflow/ui` returning nothing) — they're set here so Income/Expense become theme-aware
*through* `MaterialTheme.colorScheme`, which every screen task from Task 10 onward switches its
`Income`/`Expense` imports to read from, instead of the old flat dark-only top-level `val`s.

- [ ] **Step 5: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Fails — `GlassTier.kt`, every `Glass*.kt` component, and every screen still reference
the now-deleted `Income`/`Expense`/`Warning`/`GlassFill`/`GlassGlow`/`GlassBorderTop`/
`GlassBorderBottom`/`GlassShadow`/`Accent`/`OnAccent` top-level vals. This is expected and
resolved by Tasks 5–20 — do not try to make this one task compile in isolation; it establishes
the token layer every later task builds on. Skip straight to Task 2.

---

## Task 2: Typography additions

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Type.kt`

**Interfaces:**
- Produces: `MoneyFigureLarge: TextStyle` (28sp mono semibold, for Home's hero totals),
  `Typography.labelMedium` now explicitly styled (12sp sans medium) instead of falling back to
  M3's unstyled default.

- [ ] **Step 1: Add `labelMedium` to the `Typography(...)` block and `MoneyFigureLarge` below it**

In the `Typography(...)` constructor call, add (alongside the existing `labelSmall` entry):

```kotlin
    labelMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
```

After the `Typography(...)` block (next to the existing `MoneyFigure` val), add:

```kotlin
// Hero-scale money figure — Home screen's month income/expense totals only. Everywhere else
// (transaction rows, budget progress, report totals) keeps using [MoneyFigure].
val MoneyFigureLarge = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp
)
```

- [ ] **Step 2: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL (this file has no dependency on Task 1's not-yet-fixed callers).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/theme
git commit -m "Add minimalist color tokens, ExtendedColors, light/dark theme, tightened radius scale, typography additions"
```

---

## Task 3: `ConfirmButton` shared component

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/ConfirmButton.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Consumes: `MaterialTheme.extendedColors` (Task 1), `MaterialTheme.colorScheme.secondary` =
  Income (Task 1).
- Produces: `@Composable fun ConfirmButton(enabled: Boolean, onClick: () -> Unit, modifier:
  Modifier = Modifier)` — consumed by Tasks 9, 11, 13, 14, 17, 18, and `CalendarDialog` (Task 4).

- [ ] **Step 1: Add the string**

`values/strings.xml`, near `action_save`/`action_cancel`:
```xml
<string name="action_confirm">Konfirmo</string>
```
`values-en/strings.xml`:
```xml
<string name="action_confirm">Confirm</string>
```

- [ ] **Step 2: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R

/**
 * Shared primary/affirmative action for every dialog and form — a true pill regardless of
 * height, per the minimalist design system's confirm-button pattern (spec §7). Label is always
 * "Confirm", not "Save"/"Add"/"Import" — one predictable "ready to submit" signal app-wide,
 * deliberate per the spec rather than an oversight.
 */
@Composable
fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.extendedColors.onConfirm,
            disabledContainerColor = MaterialTheme.extendedColors.disabledFill,
            disabledContentColor = MaterialTheme.extendedColors.disabledContent
        )
    ) {
        if (enabled) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        }
        Text(stringResource(R.string.action_confirm))
    }
}
```

- [ ] **Step 3: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Still fails on the same pre-existing Task-1-dependent errors as Task 1 Step 5 — this
file itself introduces no new error (it only reads `MaterialTheme.extendedColors`/`.secondary`,
both already defined by Task 1). Confirm no *new* errors are reported for `ConfirmButton.kt`
specifically.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/ConfirmButton.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "Add shared ConfirmButton component"
```

---

## Task 4: `CalendarDialog` component + `DateField` rewrite

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/CalendarDialog.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/DateField.kt`
- Test: `app/src/test/java/com/example/financeflow/ui/CalendarGridTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3), `Radius.large` (Task 1), `currentAppLocale()` (existing,
  `locale/LocaleFormats.kt`).
- Produces: `@Composable fun CalendarDialog(initialDate: LocalDate, onConfirm: (LocalDate) ->
  Unit, onDismiss: () -> Unit)`, and a pure helper `fun calendarGridStart(month: YearMonth,
  firstDayOfWeek: DayOfWeek): LocalDate` (extracted so Step 1's test can exercise the leading-
  blank-days math without a Compose test harness).

- [ ] **Step 1: Write the failing test for the grid-start math**

```kotlin
package com.example.financeflow.ui

import com.example.financeflow.ui.components.calendarGridStart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    @Test
    fun `August 2026 starting Monday begins on the last Monday of July`() {
        // Aug 1 2026 is a Saturday; week starting Monday needs 5 leading days from July.
        val start = calendarGridStart(YearMonth.of(2026, 8), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 7, 27), start)
    }

    @Test
    fun `month starting exactly on the week's first day has no leading days`() {
        // Jun 1 2026 is a Monday.
        val start = calendarGridStart(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 6, 1), start)
    }

    @Test
    fun `Sunday-first week for a month starting on Sunday`() {
        // Nov 1 2026 is a Sunday.
        val start = calendarGridStart(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), start)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.CalendarGridTest"`
Expected: FAIL — `calendarGridStart` doesn't exist yet.

- [ ] **Step 3: Write `CalendarDialog.kt`, including the tested helper**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.example.financeflow.R
import com.example.financeflow.locale.currentAppLocale
import com.example.financeflow.ui.theme.Radius
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** First cell of a 6x7 month grid: the month's first day, walked back to the week's start. */
fun calendarGridStart(month: YearMonth, firstDayOfWeek: DayOfWeek): LocalDate {
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    return firstOfMonth.minusDays(leadingBlanks.toLong())
}

/**
 * Flat, app-styled replacement for M3's stock DatePickerDialog — java.time + Compose primitives
 * only, no new dependency (spec §8). Container follows the same policy as every other dialog:
 * Surface background, Radius.large corners, no border, no shadow.
 */
@Composable
fun CalendarDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = currentAppLocale()
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.large))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Icon(PhosphorIcons.Regular.CaretLeft, contentDescription = stringResource(R.string.calendar_previous_month))
                }
                Text(
                    text = displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.titlecase(locale) } + " " + displayedMonth.year,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                    Icon(PhosphorIcons.Regular.CaretRight, contentDescription = stringResource(R.string.calendar_next_month))
                }
            }

            Spacer(Modifier.height(8.dp))

            val orderedDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plus(it.toLong()) } }
            Row(modifier = Modifier.fillMaxWidth()) {
                orderedDays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val gridStart = remember(displayedMonth, firstDayOfWeek) { calendarGridStart(displayedMonth, firstDayOfWeek) }
            val today = remember { LocalDate.now() }

            for (week in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in 0 until 7) {
                        val day = gridStart.plusDays((week * 7 + dow).toLong())
                        val inMonth = YearMonth.from(day) == displayedMonth
                        val isSelected = day == selectedDate
                        val isToday = day == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    selectedDate = day
                                    if (!inMonth) displayedMonth = YearMonth.from(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                ConfirmButton(enabled = true, onClick = { onConfirm(selectedDate) })
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.CalendarGridTest"`
Expected: PASS, all 3 cases.

- [ ] **Step 5: Add the two new strings**

`values/strings.xml`:
```xml
<string name="calendar_previous_month">Muaji i mëparshëm</string>
<string name="calendar_next_month">Muaji tjetër</string>
```
`values-en/strings.xml`:
```xml
<string name="calendar_previous_month">Previous month</string>
<string name="calendar_next_month">Next month</string>
```

- [ ] **Step 6: Rewrite `DateField.kt` to use `CalendarDialog`**

Replace the whole file:

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.theme.Radius
import java.time.LocalDate

/** A tap-to-open single-date field: a bordered label+value row plus [CalendarDialog]. */
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = rememberDateFormat("MMM d, yyyy")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Radius.extraSmall))
            .clickable { showPicker = true }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = date.format(dateFormat), style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Rounded.DateRange, contentDescription = stringResource(R.string.date_field_change_content_description, label))
    }

    if (showPicker) {
        CalendarDialog(
            initialDate = date,
            onConfirm = { onDateChange(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}
```

- [ ] **Step 7: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Same pre-existing Task-1-dependent errors as before, no *new* ones from these two files.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/CalendarDialog.kt app/src/main/java/com/example/financeflow/ui/components/DateField.kt app/src/test/java/com/example/financeflow/ui/CalendarGridTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "Add CalendarDialog, replace DateField's stock DatePickerDialog"
```

- [ ] **Step 9: Report to user** — summarize the token layer + two new components before continuing to Task 5.

---

## Task 5: Flatten the core glass surfaces — `GlassTier`, `GlassCard`, `GlassRow`, `GlassDialog`

This is the task that resolves Task 1's compile errors for the base surface components. `GlassTier`
keeps its 4-value enum (documented in Global Constraints as a deliberate no-rename call) but its
`spec()` now resolves to solid theme colors instead of translucent glass tokens.

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/GlassTier.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassCard.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassRow.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassDialog.kt`

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme` (Task 1), `Radius.medium`/`Radius.large` (Task 1).
- Produces: `GlassCard(modifier, tier = GlassTier.Card, containerColor: Color? = null,
  cornerRadius = Radius.medium, shape, contentPadding = 20.dp, content)` — the new
  `containerColor` param is consumed by Task 10 (Home) and Task 14 (Budgets) to opt into Accent
  instead of the tier default of Surface.

- [ ] **Step 1: Rewrite `GlassTier.kt`**

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Which flat surface color a component reads by default. No longer an elevation/glass tier —
 * kept as a lightweight enum so most call sites don't need per-instance color plumbing. */
enum class GlassTier { Row, Card, Interactive, Overlay }

data class GlassTierSpec(val fill: Color)

@Composable
fun GlassTier.spec(): GlassTierSpec = when (this) {
    GlassTier.Row -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
    GlassTier.Card -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
    GlassTier.Interactive -> GlassTierSpec(fill = MaterialTheme.colorScheme.primary)
    GlassTier.Overlay -> GlassTierSpec(fill = MaterialTheme.colorScheme.surface)
}
```

- [ ] **Step 2: Rewrite `GlassCard.kt`**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.spec

/**
 * Flat solid-color surface for cards, dialogs, and other elevated content — no border, no
 * shadow, no blur. [tier] picks the default fill (Row/Card/Overlay all resolve to Surface,
 * Interactive to Primary); pass [containerColor] to override, used by the two "featured card"
 * call sites (Home summary, budget cards) that use Accent instead.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    containerColor: Color? = null,
    cornerRadius: Dp = Radius.medium,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = tier.spec()
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor ?: spec.fill)
            .padding(contentPadding),
        content = content
    )
}
```

- [ ] **Step 3: Rewrite `GlassRow.kt`**

Row tier only ever resolves to Surface, so this drops the `GlassTier`/`spec()` indirection
entirely rather than keeping an unused parameter:

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing

/** Flat Surface-background list row (transactions, categories, recurring rules) — no border. */
@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.extraSmall),
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        base = base
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
    }
    Row(modifier = base.padding(contentPadding), content = content)
}
```

- [ ] **Step 4: Rewrite `GlassDialog.kt` to stop depending on `GlassCard`**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.financeflow.ui.theme.Radius

/**
 * Flat drop-in for M3's AlertDialog — Surface background, no border, no shadow, per the
 * minimalist design system's dialog policy (spec §6).
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.large))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge) {
                title()
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    text()
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
```

- [ ] **Step 5: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Remaining errors are now only in `GlassButton.kt`, `GlassFilterChip.kt`, `GlassFab.kt`,
`GlassTextField.kt`, `GlassSegmentedControl.kt`, `QuickAddCategoryDialog.kt`, and the 9 screens —
all still referencing deleted `Color.kt` vals. Confirm no errors remain in `GlassTier.kt`,
`GlassCard.kt`, `GlassRow.kt`, or `GlassDialog.kt` themselves.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/theme/GlassTier.kt app/src/main/java/com/example/financeflow/ui/components/GlassCard.kt app/src/main/java/com/example/financeflow/ui/components/GlassRow.kt app/src/main/java/com/example/financeflow/ui/components/GlassDialog.kt
git commit -m "Flatten GlassTier/GlassCard/GlassRow/GlassDialog to solid theme colors, no border/shadow/blur"
```

---

## Task 6: Flatten interactive components — `GlassButton`, `GlassFilterChip`, `GlassFab`

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassButton.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassFilterChip.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt`

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme.primary`/`.onPrimary`/`.onSurface`/`.outline` (Task 1).
- Signatures unchanged for all three — every call site (Home/Transactions/Recurring/Categories
  FABs, Settings' export/import buttons, Add/Edit Transaction's category chips) keeps compiling
  with no changes needed here.

- [ ] **Step 1: Rewrite `GlassButton.kt`**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/** Flat drop-in for M3's Button — solid Primary fill, no border, per the minimalist policy. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        content = content
    )
}
```

- [ ] **Step 2: Rewrite `GlassFilterChip.kt`**

Unselected: border-only, no fill. Selected: solid Primary fill, no border (`selectedBorderWidth =
0.dp` is how M3's `filterChipBorder` expresses "no border for this state").

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Flat drop-in for M3's FilterChip — unselected is border-only (no fill), selected is a solid
 * Primary fill with no border, per the minimalist policy's chip rules (spec §6).
 */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.outline,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}
```

- [ ] **Step 3: Rewrite `GlassFab.kt`**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Shared floating action button — solid Primary fill, no border, no shadow, per the minimalist
 * policy. Larger than M3's default (64dp vs 56dp) for an easier thumb target; used everywhere
 * the app needs a primary "add" action (Home, Transactions, Recurring, Categories).
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            icon()
        }
    }
}
```

- [ ] **Step 4: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Remaining errors confined to `GlassTextField.kt`, `GlassSegmentedControl.kt`,
`QuickAddCategoryDialog.kt`, and the 9 screens.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassButton.kt app/src/main/java/com/example/financeflow/ui/components/GlassFilterChip.kt app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt
git commit -m "Flatten GlassButton/GlassFilterChip/GlassFab to solid Primary, no border/shadow"
```

---

## Task 7: Flatten `GlassTextField`

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassTextField.kt`

**Interfaces:**
- Drops the `tier: GlassTier = GlassTier.Card` parameter — confirmed via
  `grep -rn "GlassTextField(" app/src/main/java` that no call site passes `tier =` explicitly, so
  this is a safe removal with zero call-site changes needed.

- [ ] **Step 1: Rewrite the file**

Text fields are border-only under the minimalist policy (spec §6) — M3's own
`OutlinedTextFieldDefaults.colors()` default is already border-only with no container fill, so
this file's entire reason to exist (glass tinting) goes away; it's kept as a thin named wrapper
purely so the ~8 call sites across the app don't need import/call changes.

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Thin drop-in for M3's OutlinedTextField — border-only per the minimalist policy (spec §6),
 * kept as a named wrapper so call sites don't need to change. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors()
    )
}
```

- [ ] **Step 2: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: No new errors from this file; remaining errors unchanged from Task 6 Step 4 minus
anything specifically about `GlassTextField`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassTextField.kt
git commit -m "Flatten GlassTextField to border-only, drop unused tier param"
```

---

## Task 8: Flatten `GlassSegmentedControl`

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassSegmentedControl.kt`

- [ ] **Step 1: Rewrite the file**

Outer container: Surface, no border. Selected segment: solid Primary (the old `.copy(alpha =
0.9f)` becomes full opacity). Unselected segment: transparent (unchanged).

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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

/**
 * Flat Surface-background segmented control — selected segment is a solid Primary fill. Used for
 * every segmented control in the app (type toggles, tabs, period pickers, settings rows).
 */
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
```

- [ ] **Step 2: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Remaining errors confined to `QuickAddCategoryDialog.kt` and the 9 screens — every
shared component file now compiles clean on its own.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassSegmentedControl.kt
git commit -m "Flatten GlassSegmentedControl to Surface container, solid Primary selected segment"
```

- [ ] **Step 4: Report to user** — the full component layer is now flat; screens are next.

---

## Task 9: `QuickAddCategoryDialog` — `ConfirmButton` swap, drop selection wash

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/QuickAddCategoryDialog.kt`

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3, same package, no import needed).

- [ ] **Step 1: Remove the icon-grid selection background wash**

In the icon grid's `Box` modifier chain (around the `selectionRing(...)` call), delete this line
entirely:
```kotlin
.background(
    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = GlassAlpha.selectedTint) else Color.Transparent
)
```
Selection is now shown solely by the existing `.selectionRing(selected = selected, shape =
CircleShape, color = MaterialTheme.colorScheme.primary)` call directly above it — per spec §6,
selection rings are an explicitly-preserved mechanism, just recolored to Primary (already true
here, no change needed to that line).

Remove the now-unused imports: `androidx.compose.ui.graphics.Color` and
`com.example.financeflow.ui.theme.GlassAlpha`.

- [ ] **Step 2: Replace the confirm `TextButton` with `ConfirmButton`**

Replace:
```kotlin
TextButton(
    enabled = name.isNotBlank(),
    onClick = { onSave(Category(name = name.trim(), type = type, icon = icon)) }
) { Text(stringResource(R.string.action_save)) }
```
with:
```kotlin
ConfirmButton(
    enabled = name.isNotBlank(),
    onClick = { onSave(Category(name = name.trim(), type = type, icon = icon)) }
)
```
Leave the dismiss `TextButton` ("Cancel") unchanged — per spec §7, only the primary/affirmative
action is replaced.

- [ ] **Step 3: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's own errors resolved. Remaining errors confined to the 9 screens.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/QuickAddCategoryDialog.kt
git commit -m "QuickAddCategoryDialog: ConfirmButton, drop redundant selection-wash background"
```

---

## Task 10: Home screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt`

**Interfaces:**
- Consumes: `GlassCard`'s new `containerColor` param (Task 5), `MaterialTheme.extendedColors.
  accent` (Task 1), `MaterialTheme.colorScheme.secondary`/`.tertiary` (Task 1), `MoneyFigureLarge`
  (Task 2).

- [ ] **Step 1: Give the summary card the Accent background**

The month income/expense summary `GlassCard` (L95) is the spec's explicit "featured card" example
— add `containerColor = MaterialTheme.extendedColors.accent` to its call so it reads as Accent
instead of the tier default of Surface, distinguishing it from ordinary list rows.

- [ ] **Step 2: Swap the `Income`/`Expense` imports**

Replace `import com.example.financeflow.ui.theme.Income` / `Expense` with reads of
`MaterialTheme.colorScheme.secondary` / `.tertiary` at each use site (this file's amount-tinting
logic) — these are the same values now, just theme-aware instead of frozen dark-only constants.

- [ ] **Step 3: Apply `MoneyFigureLarge` to the hero totals**

The summary card's income/expense total `Text`s (the large numbers, distinct from any per-row
`MoneyFigure` amounts elsewhere in the file) should use `MoneyFigureLarge` instead of whatever
style they currently use — this is the "Amount — hero (Home totals)" row from spec §4's
typography table.

- [ ] **Step 4: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt
git commit -m "Home screen: Accent summary card, theme-aware Income/Expense, hero money figure"
```

- [ ] **Step 6: Report to user** — summarize before continuing to Task 11.

---

## Task 11: Add/Edit Transaction screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt`

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3).

- [ ] **Step 1: Move the confirm action from the TopAppBar to a bottom-anchored `ConfirmButton`**

This is the spec's explicit named case (§7). Currently: `TopAppBar` has a `navigationIcon` back
`IconButton` (unaffected) and an `actions` slot with `IconButton(onClick = ::save, enabled =
canSave) { Icon(Icons.Rounded.Check, ...) }` (the one being replaced).

Change the screen's `Scaffold` to add `bottomBar = { ConfirmButton(enabled = canSave, onClick =
::save, modifier = Modifier.fillMaxWidth().padding(16.dp)) }`, and remove the `actions` block
from the `TopAppBar` entirely (keep only the back-arrow `navigationIcon`). Apply the `Scaffold`'s
`innerPadding` to the form content the same way it already is, so the form's scrollable content
doesn't render underneath the new bottom bar.

- [ ] **Step 2: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved (its `GlassTextField`/`GlassSegmentedControl`/
`GlassFilterChip` usages already compile clean from Tasks 6–8; this step is purely the
TopAppBar→bottom-bar restructure).

- [ ] **Step 3: Manually verify**

Run the app, open Add Transaction: confirm the checkmark is gone from the top bar, a full-width
"Confirm" pill sits at the bottom, it's disabled (grey) until the form is valid, and scrolling the
form doesn't hide fields behind the bottom bar.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt
git commit -m "Add/Edit Transaction: move confirm action to bottom-anchored ConfirmButton"
```

---

## Task 12: Transactions screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt`

This is the screen the original report named directly: search bar, category dropdown, and date
range bar.

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme.outline`/`.surface` (Task 1), `Radius.extraSmall` (Task 1).

- [ ] **Step 1: Search bar**

The search bar is a `GlassTextField` (L100-109) — already flattened to border-only by Task 7, no
call-site change needed here. Just verify it renders border-only, no fill, after Task 7.

- [ ] **Step 2: Category dropdown**

`CategoryDropdown`'s anchor field (L189-196) is also a `GlassTextField` — same as Step 1, already
flat via Task 7. Verify only.

- [ ] **Step 3: Date range bar — the one genuinely bespoke fix in this file**

`DateRangeField` (L222-236) currently wraps its label+value+calendar-icon row in a plain
`GlassCard` (translucent fill + gradient border + shadow pre-Task-5, now flat-Surface-fill +
still no border post-Task-5). For visual consistency with `DateField`'s own border-only treatment
(a single-date field elsewhere in the app), replace the `GlassCard` wrapper here with the same
border-only pattern: a `Row` with `.border(1.dp, MaterialTheme.colorScheme.outline,
RoundedCornerShape(Radius.extraSmall))` instead of a filled card, keeping the same label/value/
icon content.

- [ ] **Step 4: `DateRangePickerDialog` container**

Its container `GlassCard(tier = GlassTier.Overlay)` (L253-257) wraps the stock M3
`DateRangePicker`. Leave it as `GlassCard` (already flat-Surface via Task 5, no change needed
here) — do **not** attempt to replace the stock M3 `DateRangePicker` itself with a custom
component. `CalendarDialog` (Task 4) is explicitly single-date only (spec §8 scopes it to
`LocalDate`, and `DateRangeField`/`DateRangePickerDialog` need a *range*); building a custom
range-calendar grid is out of scope for this pass and not requested. This is the one documented
exception in the Global Constraints' acceptance-bar grep.

Per spec §7's explicit carve-out ("`GlassButton`/`GlassDialog`'s existing `TextButton`
"Cancel"/"Clear"/"Apply" actions are unaffected"), leave the three `TextButton`s ("Clear",
"Cancel", "Apply") exactly as they are — do not convert "Apply" to `ConfirmButton`.

- [ ] **Step 5: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 6: Manually verify**

Run the app, open Transactions: search bar and category dropdown show a border with no fill; the
date range bar shows a border with no fill (matches `DateField`'s look elsewhere); tapping it
still opens the M3 range picker, whose Clear/Cancel/Apply buttons are unchanged.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt
git commit -m "Transactions screen: flatten date range bar to border-only, matching DateField"
```

---

## Task 13: Categories screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/CategoryStyle.kt`

This is the other screen the original report named directly — the "translucent brownish tint" on
every category card.

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3), `MaterialTheme.colorScheme.tertiaryContainer` (Task 1, =
  ExpenseContainer).

- [ ] **Step 1: The card tint itself is already fixed by Task 5**

`CategoryRow`'s `GlassRow` wrapper (L241) and the icon-grid/color-grid dialog's `GlassCard`/
`GlassDialog` are flat-Surface as of Tasks 5 and 9 — no further change needed for the base tint.
This task handles the remaining bespoke alpha-wash spots Task 5 didn't touch.

- [ ] **Step 2: Remove the nested `GlassCard` wrapper around the color-swatch grid**

The `AddEditCategoryDialog`'s color-swatch `FlowRow` is wrapped in its own `GlassCard(
contentPadding = 12.dp) { ... }` (L356). A Surface-colored card nested inside an already
Surface-colored dialog is redundant under the flat policy (no double-surfacing). Remove the
`GlassCard` wrapper, keep the `FlowRow(...)` content as a bare composable directly inside the
dialog's `Column`.

- [ ] **Step 3: Swipe-to-delete background — swap alpha wash for the solid container color**

Line 222: `.background(Expense.copy(alpha = GlassAlpha.destructiveTint), MaterialTheme.shapes.
small)` → `.background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small)`
(tertiaryContainer = ExpenseContainer per Task 1's mapping — the exact "soft expense-tinted
surface" tone this alpha wash was trying to fake).

- [ ] **Step 4: Category icon swatch badge — full opacity, contrast-picked icon tint**

`CategoryRow`'s icon badge (L252) currently: `.background(swatch.copy(alpha = GlassAlpha.
containerTint), CircleShape)` with the icon drawn at default tint on top. Change to full-opacity
swatch fill with a luminance-based icon tint so it stays legible against any category color:

```kotlin
.background(swatch, CircleShape)
```
and on the `Icon(...)` inside it, add `tint = if (swatch.luminance() > 0.5f) Color.Black else
Color.White` (needs `import androidx.compose.ui.graphics.luminance`). This also makes the
category's color-coding bolder and more legible than the old low-alpha wash did.

- [ ] **Step 5: Dialog selection washes — drop, rely on the selection ring alone**

Wherever the icon grid or color-swatch grid in `AddEditCategoryDialog` draws a `.background(...
.copy(alpha = GlassAlpha.selectedTint) ...)` for the selected state, delete that background
modifier — keep only the existing `.selectionRing(...)` border call, same treatment as Task 9's
`QuickAddCategoryDialog` fix.

- [ ] **Step 6: `AddEditCategoryDialog`'s confirm action → `ConfirmButton`**

Replace the `TextButton(enabled = name.isNotBlank()) { Text(stringResource(R.string.
action_save)) }` confirm block (L403-422) with `ConfirmButton(enabled = name.isNotBlank(), onClick
= { ... })`, same pattern as Task 9. Leave the dismiss "Cancel" `TextButton` unchanged.

- [ ] **Step 7: Swap the `Expense` import**

Replace `import com.example.financeflow.ui.theme.Expense` with reads of `MaterialTheme.
colorScheme.tertiary` at its use site(s) in this file.

- [ ] **Step 8: `CategoryStyle.kt`'s fallback color**

`toCategoryColor()`'s fallback (`?: OnSurfaceMuted`) reads the dark-only top-level val. Since this
is a plain (non-`@Composable`) extension function, it can't read `MaterialTheme.colorScheme`
directly — change its signature to `fun String?.toCategoryColor(fallback: Color): Color` (drop
the internal `OnSurfaceMuted` default), and update its one call site in `CategoryRow` to pass
`MaterialTheme.colorScheme.onSurfaceVariant` explicitly. Remove the now-unused
`import com.example.financeflow.ui.theme.OnSurfaceMuted`.

- [ ] **Step 9: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: Both files' errors resolved.

- [ ] **Step 10: Manually verify**

Run the app, open Categories: every card is a flat solid surface color, no tint/gradient. Icon
badges are solid-colored circles with legible icon tint. Swipe-to-delete shows a solid muted-red
background. Add/Edit dialog's icon and color pickers show selection via ring only, no wash; Save
is now a "Confirm" pill.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt app/src/main/java/com/example/financeflow/ui/components/CategoryStyle.kt
git commit -m "Categories screen: flatten remaining alpha washes, ConfirmButton, drop nested card"
```

---

## Task 14: Budgets screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt`

This is the screen the original report named directly — the "two-tone gradient" on budget cards.

**Interfaces:**
- Consumes: `GlassCard`'s new `containerColor` param (Task 5), `MaterialTheme.extendedColors.
  accent`/`.warning`/`.warningContainer` (Task 1), `MaterialTheme.colorScheme.secondaryContainer`/
  `.tertiaryContainer` (Task 1), `ConfirmButton` (Task 3).

- [ ] **Step 1: The blurred top-strip gradient is already gone as of Task 5**

`GlassCard` no longer paints the blurred glow-patch `Box` at all (Task 5 removed it entirely from
the component), so the literal "lighter header strip fading into darker body" effect is already
gone. This task adds the two remaining pieces the spec calls for.

- [ ] **Step 2: Give `BudgetCard` the Accent background**

Same treatment as Task 10's Home summary card — `BudgetCard`'s `GlassCard` (L178) is the spec's
other named "featured card" example. Add `containerColor = MaterialTheme.extendedColors.accent`.

- [ ] **Step 3: Progress track — solid container colors instead of an alpha wash**

`BudgetCard`'s `LinearProgressIndicator` (L215) currently: `trackColor = statusColor.copy(alpha =
GlassAlpha.trackTint)`. Since `statusColor` already varies by the three-state logic (< 0.8 →
Income, 0.8–1.0 → Warning, ≥ 1.0 → Expense), replace with the matching solid container per state:
```kotlin
trackColor = when (status) {
    BudgetStatus.OK -> MaterialTheme.colorScheme.secondaryContainer
    BudgetStatus.WARNING -> MaterialTheme.extendedColors.warningContainer
    BudgetStatus.OVER -> MaterialTheme.colorScheme.tertiaryContainer
}
```
(adjust the `when` to match this file's actual three-state variable/enum names — the exact
"progress-bar tracks — solid, not alpha" mapping is what spec §3's IncomeContainer/
ExpenseContainer/WarningContainer row is for.)

- [ ] **Step 4: `SetBudgetDialog`'s confirm action → `ConfirmButton`**

Replace the `TextButton(enabled = canSave) { Text(stringResource(R.string.action_save)) }` block
(L328-335) with `ConfirmButton(enabled = canSave, onClick = { ... })`.

- [ ] **Step 5: Swap the `Income`/`Expense`/`Warning` imports**

Replace `import com.example.financeflow.ui.theme.Expense` / `Income` / `Warning` with
`MaterialTheme.colorScheme.tertiary` / `.secondary` / `MaterialTheme.extendedColors.warning` at
their use sites (the three-state text/icon coloring).

- [ ] **Step 6: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 7: Manually verify**

Run the app, open Budgets (via Reports' Budgets tab): every card is a flat single Accent color,
no gradient. Progress tracks show solid pale-tinted backgrounds matching each card's status.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt
git commit -m "Budgets screen: Accent cards, solid progress-track containers, ConfirmButton"
```

---

## Task 15: Reports screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt`

This screen only uses `GlassSegmentedControl` (Overview/Budgets tab, period picker,
`TransactionTypeToggle`) and `GlassRow` (daily report list rows) — both already fully flattened
by Tasks 5 and 8 with unchanged call signatures. This task is a verification + import-cleanup
pass, not a restructure.

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme.secondary`/`.tertiary` (Task 1).

- [ ] **Step 1: Swap the `Income`/`Expense` imports**

Replace `import com.example.financeflow.ui.theme.Expense` / `Income` with `MaterialTheme.
colorScheme.tertiary` / `.secondary` at their use sites (chart bar coloring, category pie
coloring, wherever this file currently tints by income/expense).

- [ ] **Step 2: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 3: Manually verify**

Run the app, open Reports: Overview/Budgets tab switch, period picker, and daily list rows all
show flat colors — segmented controls' selected segment is a solid Primary pill, list rows are
flat Surface.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt
git commit -m "Reports screen: theme-aware Income/Expense colors"
```

---

## Task 16: Recurring Rules screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt`

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme.secondary`/`.tertiary` (Task 1).

- [ ] **Step 1: Drop the Switch's custom alpha-wash track color**

`RecurringRuleRow`'s active/paused `Switch` (L178) has a custom `colors = SwitchDefaults.colors(
checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = GlassAlpha.selectedTint))`.
M3's own `SwitchDefaults` already uses a solid `colorScheme.primary` for the checked track by
default — remove the whole `colors = SwitchDefaults.colors(...)` override and let the `Switch`
use its stock M3 defaults.

- [ ] **Step 2: Swap the `Income`/`Expense` imports**

Replace `import com.example.financeflow.ui.theme.Expense` / `Income` with `MaterialTheme.
colorScheme.tertiary` / `.secondary` at their use sites.

- [ ] **Step 3: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 4: Manually verify**

Run the app, open Recurring Rules: rows are flat Surface (`GlassRow`, already flat via Task 5);
the active/paused switch's checked state is a solid-color track, not a translucent wash.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt
git commit -m "Recurring Rules screen: solid switch track, theme-aware Income/Expense"
```

---

## Task 17: Add/Edit Recurring Rule screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditRecurringRuleScreen.kt`

Same pattern as Task 11 — the spec names this screen explicitly alongside Add/Edit Transaction.

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3).

- [ ] **Step 1: Move the confirm action from the TopAppBar to a bottom-anchored `ConfirmButton`**

`TopAppBar`'s `actions` currently has `IconButton(onClick = ::save, enabled = canSave) {
Icon(Icons.Rounded.Check, ...) }` (L143-146); the back-arrow `IconButton` (L138-140) is
unaffected. Same restructure as Task 11 Step 1: add `bottomBar = { ConfirmButton(enabled =
canSave, onClick = ::save, modifier = Modifier.fillMaxWidth().padding(16.dp)) }` to the `Scaffold`,
remove the `actions` block, apply `innerPadding` to the form content.

- [ ] **Step 2: Leave the 4 stock `SingleChoiceSegmentedButtonRow`/`SegmentedButton` groups as-is**

This screen's expense/income, personal/business, currency, and frequency pickers use stock M3
`SingleChoiceSegmentedButtonRow`, not `GlassSegmentedControl`. The spec doesn't call for swapping
these to the shared component — M3's own `SegmentedButton` selected-state fill already reads from
`MaterialTheme.colorScheme.primary`-derived tokens by default, which is theme-aware as of Task 1
with zero changes needed here. Leave them.

- [ ] **Step 3: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved.

- [ ] **Step 4: Manually verify**

Run the app, open Add/Edit Recurring Rule: same check as Task 11 — checkmark gone from the top
bar, full-width "Confirm" pill at the bottom, disabled until valid.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/AddEditRecurringRuleScreen.kt
git commit -m "Add/Edit Recurring Rule: move confirm action to bottom-anchored ConfirmButton"
```

---

## Task 18: Settings screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `ConfirmButton` (Task 3).

- [ ] **Step 1: The Categories/Recurring rows and export/import buttons are already flat**

Both `GlassCard` rows (L104, L116) resolve to flat Surface via Task 5 with no `containerColor`
override needed (they're ordinary navigation rows, not "featured cards" — Accent is reserved for
Home's summary and Budgets' cards per spec §3/§6). The two `GlassButton`s (export/import, L149/
L152) are flat solid-Primary via Task 6. No changes needed for either — verify only.

- [ ] **Step 2: Backup import-confirm dialog's action → `ConfirmButton`**

The `GlassDialog`'s affirmative action (L208-220) is currently `TextButton(onClick = {...}) {
Text(stringResource(R.string.backup_import_confirm_action)) }`. Per spec §7 this becomes
`ConfirmButton`, always enabled (there's no validity gate on this action): replace with
`ConfirmButton(enabled = true, onClick = {...})`. The dialog's "Cancel" `TextButton` (L223) is
unaffected. Note this drops the custom "Zëvendëso"/"Replace" label in favor of the shared
"Confirm" label — per spec §7 this is deliberate, not a regression; the now-unused
`backup_import_confirm_action` string can stay in `strings.xml` unreferenced or be removed (your
call — removing it is the tidier option since nothing else uses it; confirm with
`grep -rn "backup_import_confirm_action" app/src/main/java` returning nothing before deleting).

- [ ] **Step 3: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: This file's errors resolved. **This should be the last screen file with errors** —
confirm with a full `gradlew.bat compileDebugKotlin` that the whole module now builds clean.

- [ ] **Step 4: Manually verify**

Run the app, open Settings: rows and buttons all flat. Trigger a backup import (or just open the
confirm dialog) and check its affirmative action is now a "Confirm" pill.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt
git commit -m "Settings screen: ConfirmButton for backup import confirmation"
```

- [ ] **Step 6: Run the full test suite**

Run: `gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests plus Task 4's new `CalendarGridTest` pass.

---

## Task 19: Bottom navigation — flatten + active-state fix (spec §11)

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowDestination.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowApp.kt`
- Test: `app/src/test/java/com/example/financeflow/ui/FinanceFlowDestinationTest.kt`

Two independent problems named in spec §11, both still present in the current code (neither was
ever fixed by any prior pass): weak/default selected-state styling, and no active tab at all when
navigated into Categories or Recurring from Settings.

**Interfaces:**
- Produces: `fun FinanceFlowDestination.matchesCurrentRoute(currentRoute: String?): Boolean`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.financeflow.ui

import com.example.financeflow.ui.navigation.CATEGORIES_ROUTE
import com.example.financeflow.ui.navigation.FinanceFlowDestination
import com.example.financeflow.ui.navigation.RECURRING_ROUTE
import com.example.financeflow.ui.navigation.matchesCurrentRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceFlowDestinationTest {
    @Test
    fun `each destination matches its own route`() {
        FinanceFlowDestination.entries.forEach { destination ->
            assertTrue(destination.matchesCurrentRoute(destination.route))
        }
    }

    @Test
    fun `Settings matches Categories and Recurring sub-routes`() {
        assertTrue(FinanceFlowDestination.Settings.matchesCurrentRoute(CATEGORIES_ROUTE))
        assertTrue(FinanceFlowDestination.Settings.matchesCurrentRoute(RECURRING_ROUTE))
    }

    @Test
    fun `Home does not match Categories or Recurring sub-routes`() {
        assertFalse(FinanceFlowDestination.Home.matchesCurrentRoute(CATEGORIES_ROUTE))
        assertFalse(FinanceFlowDestination.Home.matchesCurrentRoute(RECURRING_ROUTE))
    }

    @Test
    fun `null route matches nothing`() {
        FinanceFlowDestination.entries.forEach { destination ->
            assertFalse(destination.matchesCurrentRoute(null))
        }
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.FinanceFlowDestinationTest"`
Expected: FAIL — `matchesCurrentRoute` doesn't exist yet.

- [ ] **Step 3: Add `matchesCurrentRoute` to `FinanceFlowDestination.kt`**

Append after the existing route constants:

```kotlin
fun FinanceFlowDestination.matchesCurrentRoute(currentRoute: String?): Boolean = when (this) {
    FinanceFlowDestination.Settings -> currentRoute == route || currentRoute == CATEGORIES_ROUTE || currentRoute == RECURRING_ROUTE
    else -> currentRoute == route
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.FinanceFlowDestinationTest"`
Expected: PASS, all 4 cases.

- [ ] **Step 5: Use it in `FinanceFlowBottomBar`, add explicit selected-state colors**

In `FinanceFlowApp.kt`'s `FinanceFlowBottomBar`, change `val isSelected = currentRoute ==
destination.route` to `val isSelected = destination.matchesCurrentRoute(currentRoute)`.

Add explicit colors to the `NavigationBarItem` call (currently relies on unconfigured M3
defaults):
```kotlin
colors = NavigationBarItemDefaults.colors(
    indicatorColor = MaterialTheme.colorScheme.primary,
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
```
(needs `import androidx.compose.material3.NavigationBarItemDefaults` and
`import androidx.compose.material3.MaterialTheme`). Keep the existing Fill/Regular icon swap as a
second, reinforcing signal on top of the color change (unchanged).

- [ ] **Step 6: Flatten the bottom bar's own container**

`FinanceFlowBottomBar`'s outer `GlassCard` (wrapping the `NavigationBar`) is already flat-Surface
via Task 5. Update its hardcoded `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)` to use the
new radius scale: `RoundedCornerShape(topStart = Radius.medium, topEnd = Radius.medium)` (needs
`import com.example.financeflow.ui.theme.Radius`).

- [ ] **Step 7: Flatten the snackbar's container**

The `SnackbarHost`'s `GlassCard(tier = GlassTier.Overlay, ...)` (in `FinanceFlowApp`) is already
flat-Surface via Task 5 — verify only, no change needed.

- [ ] **Step 8: Compile and run both new test files**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.
Run: `gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL, full suite green including both `CalendarGridTest` and
`FinanceFlowDestinationTest`.

- [ ] **Step 9: Manually verify**

Run the app: bottom nav's selected tab shows a solid Primary pill indicator with contrasting
icon/label color; navigate Settings → Categories and Settings → Recurring and confirm the
Settings tab stays visually active on both sub-screens (previously showed nothing selected).

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/navigation app/src/test/java/com/example/financeflow/ui/FinanceFlowDestinationTest.kt
git commit -m "Bottom nav: explicit Primary selected-state colors, active state on Settings sub-routes"
```

---

## Task 20: Typography audit — secondary/meta text to `labelMedium`

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/TransactionRow.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt`

Per spec §4: several screens currently use `bodyMedium` (14sp) for both moderately-important text
and truly secondary/meta text (dates, captions), collapsing a hierarchy distinction the spec
calls for. This is a per-instance judgment call, not a mechanical find/replace — convert only the
clearly-secondary cases below; leave content-bearing `bodyMedium` (e.g. a rule's descriptive
summary line) unchanged.

**Interfaces:**
- Consumes: `Typography.labelMedium`, `MaterialTheme.colorScheme.onSurfaceVariant` (both Task 1/2).

- [ ] **Step 1: `TransactionRow.kt`**

The transaction date line (`transaction.date.format(dateFormat)`, currently `bodyMedium`) is
meta/secondary relative to the category name above it — change to `style =
MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant`.

- [ ] **Step 2: `BudgetsScreen.kt`**

Audit `BudgetCard`'s caption-style lines (period label, spent-vs-limit caption text) — the ones
that are clearly secondary to the category name/progress bar, not the category name itself. Apply
the same `labelMedium` + `onSurfaceVariant` treatment.

- [ ] **Step 3: `RecurringRulesScreen.kt`**

`RecurringRuleRow`'s next-due-date / frequency line is meta text relative to the rule's label —
apply the same treatment.

- [ ] **Step 4: `HomeScreen.kt`**

The recent-transactions list's date captions (if styled separately from `TransactionRow`, which
already got fixed in Step 1) — apply the same treatment where applicable.

- [ ] **Step 5: `ReportsScreen.kt`**

Chart axis labels / period captions that are clearly secondary — apply the same treatment.

- [ ] **Step 6: Compile**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Manually verify**

Run the app and spot-check Transactions, Budgets, Recurring Rules, Home, and Reports: secondary/
meta text (dates, captions) now reads visibly smaller and more muted than primary row content,
without any row's most important text (category name, amount, rule label) losing prominence.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/TransactionRow.kt app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt
git commit -m "Apply labelMedium typography role to secondary/meta text across list screens"
```

---

## Task 21: Delete `GlassAlpha.kt`, update CLAUDE.md, final verification

**Files:**
- Delete: `app/src/main/java/com/example/financeflow/ui/theme/GlassAlpha.kt`
- Modify: `app/CLAUDE.md`

**Interfaces:** None — this is cleanup + documentation, no new code.

- [ ] **Step 1: Verify `GlassAlpha` has zero remaining references**

Run: `grep -rn "GlassAlpha" app/src/main/java`
Expected: no output. (Tasks 9, 13, 14, 16 each removed their `GlassAlpha.*` usage; if this grep
finds anything, go fix that call site first — do not delete the file with live references.)

- [ ] **Step 2: Delete the file**

```bash
git rm app/src/main/java/com/example/financeflow/ui/theme/GlassAlpha.kt
```

- [ ] **Step 3: Update `app/CLAUDE.md`'s Design & Styling section**

Replace the "Glassmorphism (modern frosted-glass look)" subsection (the one describing the
4-tier `GlassTier` elevation system with fill/blur/shadow) with a description of the flat system:

```markdown
**Minimalist flat design (as of the 2026-08-23 pass):**
- Solid colors only — no transparency, no blur, no shadows anywhere in the UI.
- Light/dark palette follows the system setting (`isSystemInDarkTheme()`); see
  `ui/theme/Color.kt`/`ui/theme/Theme.kt`/`ui/theme/ExtendedColors.kt` for both palettes.
- Component policy is "background XOR border, zero shadow" — which one applies is decided by
  component *type*: list rows and dialogs get a Surface fill and no border; buttons/FAB/selected
  chips/selected segments get a solid Primary fill and no border; unselected chips and text
  fields are border-only, no fill. Featured cards (Home's summary, Budgets' cards) get an Accent
  fill instead of Surface — everything else that used to read as "glass" now reads as flat
  Surface.
- `GlassCard`/`GlassRow`/`GlassButton`/`GlassTextField`/`GlassSegmentedControl`/`GlassDialog`/
  `GlassFilterChip`/`GlassFab` keep their names from the prior glass-era pass (renaming them
  would touch every call site for no behavioral benefit) but are now flat solid-color
  implementations — none of them draw a gradient border, blur, or shadow.
- `ConfirmButton` (`ui/components/ConfirmButton.kt`) is the one shared primary/affirmative action
  for every dialog and form — always labeled "Confirm", a pill shape, Income-green when enabled.
- `CalendarDialog` (`ui/components/CalendarDialog.kt`) replaces M3's stock `DatePickerDialog` for
  single-date fields (`DateField`); the Transactions screen's date *range* picker still uses M3's
  stock `DateRangePicker`, which has no flat custom replacement in this codebase.
```

- [ ] **Step 4: Full compile + full test suite**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.
Run: `gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL, full suite green (existing 6 test classes + `CalendarGridTest` +
`FinanceFlowDestinationTest`).

- [ ] **Step 5: Run the acceptance-bar grep from Global Constraints**

Run:
```bash
grep -rn "Brush\.verticalGradient\|Brush\.horizontalGradient\|\.blur(\|\.copy(alpha" app/src/main/java/com/example/financeflow/ui
```
Expected: zero matches, **except** any hits inside `TransactionsScreen.kt`'s
`DateRangePickerDialog`/stock M3 `DateRangePicker` usage (the one documented, deliberate
exception from Task 12 Step 4 — M3's own internals, not app code, and out of scope per spec §8's
single-date-only `CalendarDialog` contract). If anything else shows up, that's a real miss — go
fix it before calling this pass done.

- [ ] **Step 6: Manually verify end-to-end, both light and dark**

Run the app in both a light-mode and dark-mode emulator/device session (or toggle system dark
mode mid-session) and walk every screen: Home, Transactions (+ Add/Edit), Categories, Budgets,
Reports, Recurring Rules (+ Add/Edit), Settings. Confirm every surface is a flat solid color in
both modes, text stays legible (no low-contrast surprises from the light-mode retune), and the
confirm-button pill's text color is correct in both modes (white-on-dark-green in light mode,
near-black-on-pale-green in dark mode — the exact risk called out in spec §12).

- [ ] **Step 7: Commit**

```bash
git add app/CLAUDE.md
git commit -m "Delete dead GlassAlpha, update CLAUDE.md's Design & Styling section for the minimalist system"
```

- [ ] **Step 8: Report to user** — the full pass is complete: token layer, all 8 `Glass*`
  components, 2 new components (`ConfirmButton`, `CalendarDialog`), all 9 screens, bottom nav,
  typography audit, and final cleanup. Zero remaining transparency/gradient/blur effects except
  the one documented M3-internal exception.

---

## Self-Review Notes

- **Spec coverage:** §1 goals → every task. §3 color tokens → Task 1. §4 typography → Tasks 2, 10
  (hero figure), 20 (audit). §5 radius → Task 1. §6 component policy → Tasks 5–9 (shared
  components) + per-screen tasks applying `containerColor`/import swaps. §7 ConfirmButton → Task
  3 + call sites in Tasks 9, 11, 13, 14, 17, 18. §8 CalendarDialog → Task 4. §9 rollout order →
  Tasks 10–18 follow the named Home→Settings sequence. §10 testing → compile+manual per task, plus
  real unit tests for the two genuinely-pure-logic additions (Tasks 4, 19). §11 bottom nav → Task
  19. §12 risks → explicitly re-checked in Task 21 Step 6.
- **Placeholder scan:** every step either gives exact code or a specific, locatable instruction
  (file, line reference from the research forks, exact before/after). No "TBD"/"add validation"/
  "similar to Task N" phrasing.
- **Type consistency:** `GlassCard(tier, containerColor, cornerRadius, shape, contentPadding,
  content)` (Task 5) is the signature every later `containerColor =` call site (Tasks 10, 14)
  uses. `ConfirmButton(enabled, onClick, modifier)` (Task 3) matches every call site (Tasks 4, 9,
  11, 13, 14, 17, 18). `CalendarDialog(initialDate, onConfirm, onDismiss)` (Task 4) matches
  `DateField`'s call (same task). `MaterialTheme.extendedColors.{accent,warning,warningContainer,
  onWarning,disabledFill,disabledContent,onConfirm}` (Task 1) matches every consumer.
- **Compilability ordering:** Tasks 1–4 intentionally do not compile clean in isolation (Task 1
  Step 5 says so explicitly) because the shared token layer and the 8 `Glass*` components are
  mutually coupled through `GlassTierSpec`'s fields — Task 5 is where `GlassTier`/`GlassCard`/
  `GlassRow`/`GlassDialog` become consistent again, Tasks 6–8 finish the remaining components,
  and the module is verified compiling clean again at Task 9 (no screen changes needed yet) and
  after every subsequent screen task.
