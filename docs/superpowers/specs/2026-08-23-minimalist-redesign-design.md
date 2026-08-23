# Minimalist Redesign — Design System & Screen-by-Screen Rework

Status: approved in chat 2026-08-23. Implementing now. Supersedes
`docs/superpowers/specs/2026-08-23-full-redesign-design.md` (glassmorphism) — see that file's
"State at supersession" section for the exact scope of what this rework touches.

## 1. Goals

Replace the glassmorphism design system with a flat, minimalist one: solid colors only (no
transparency, no blur, no shadows), a light/dark palette that follows the system setting, a clear
typography hierarchy, one consistent corner-radius scale, and a unified pill-shaped confirm-button
pattern across every dialog and form. Presentation-layer only — no ViewModel/Room/query changes,
same as every prior pass.

## 2. Out of scope

- No manual light/dark toggle — `FinanceFlowTheme` follows `isSystemInDarkTheme()`. A manual
  override needs a persisted preference and a Settings row; not trivial, not requested.
- No new dependencies. The calendar dialog is built from plain `java.time` + Compose primitives.
- No ViewModel/Room/DAO/query logic changes.
- No nav structure changes.

## 3. Color tokens

Two full palettes (`ui/theme/Color.kt`), selected by `isSystemInDarkTheme()`:

| Token | Light | Dark |
|---|---|---|
| Background | `#FFF4F4` | `#222831` |
| Surface (rows, ordinary cards, dialogs, bottom nav) | `#FFD6E0` | `#393E46` |
| Accent (featured cards only — Home summary, budget cards) | `#FFD6E0` (same as Surface) | `#DFD0B8` |
| Primary (buttons/FAB/selected chip/selected segment) | `#BDB2FF` | `#BDB2FF` (same both modes) |
| OnPrimary | near-black | near-black |
| OnBackground / OnSurface | near-black | near-white |
| OnSurfaceMuted (secondary/meta text) | mid grey | mid grey |
| Income (positive amounts, confirm-button-enabled fill) | `#2F6B4F` | `#7FB88F` (unchanged) |
| Expense (negative amounts) | `#9C4419` | `#D9825F` (unchanged) |
| Warning (budget 80–99%) | `#855708` | `#D9A441` (unchanged) |
| IncomeContainer / ExpenseContainer / WarningContainer (progress-bar tracks — solid, not alpha) | pale tint of each hue | existing dark-tuned values, kept |
| DisabledFill (confirm button, invalid) | `#D9D2D2` | `#4A4F57` |
| DisabledContent | `#8A8080` | `#9AA0A8` |
| OnConfirm (confirm button, enabled — mode-dependent, not a fixed value) | white | near-black |

**Why the light-mode semantic colors changed:** the existing Income/Expense/Warning hues (tuned
against the old dark backdrop) contrast at only 1.7–2.7:1 against the new `#FFF4F4`/`#FFD6E0` —
well under WCAG AA's 4.5:1 for text. Verified with a WCAG relative-luminance calculation
(`(L_lighter + 0.05) / (L_darker + 0.05)`) before picking replacements; the new light values hit
4.7–6.0:1 against both `#FFF4F4` and the harder case, `#FFD6E0`. Dark-mode values are unchanged —
they already pass (4.7–6.6:1) against the new dark background/surface, no reason to touch them.

**Why OnConfirm is mode-dependent, not a constant:** the confirm button's enabled fill is each
mode's own Income color — light mode's is a deep forest green (`#2F6B4F`, needs white text/icon:
6.3:1 vs. 3.3:1 for black), dark mode's is a pale pastel green (`#7FB88F`, needs near-black:
9.2:1 vs. 2.3:1 for white). A single fixed text color would fail one of the two modes.

## 4. Typography

Extends the existing serif-display/sans-body/monospace-money identity (kept — it's a deliberate
"ledger" identity choice from the original design pass, independent of glass vs. flat) with an
explicit role table, filling the M3 slots that currently fall back to unstyled defaults:

| Role | Style | Spec |
|---|---|---|
| Screen title | `headlineMedium` | 22sp serif medium (existing) |
| Section header | `titleLarge` | 18sp sans semibold (existing) |
| Card/row title | `bodyLarge` | 16sp sans (existing) |
| Amount — hero (Home totals) | `MoneyFigureLarge` (new) | 28sp mono semibold |
| Amount — row-level | `MoneyFigure` | 20sp mono semibold (existing) |
| Secondary/meta text (dates, budget captions) | `labelMedium` (new) | 12sp sans medium, `OnSurfaceMuted` |
| Field labels | M3 default | handled by `OutlinedTextField`'s own label slot |

"Apply consistently" means auditing each screen's `Text(...)` calls against this table — today
several screens use `bodyMedium` (14sp) for both moderately-important text (rule summary lines)
and truly secondary text (captions), collapsing the hierarchy the brief calls out.

## 5. Corner radius

Tightened from the glass-era scale (12/16/24/28, tuned for a soft "blob" look) to a crisper
minimalist scale in `ui/theme/Radius.kt`:

| Token | Old | New |
|---|---|---|
| extraSmall | 12dp | 8dp |
| small | 16dp | 12dp |
| medium | 24dp | 16dp |
| large | 28dp | 20dp |

Applied everywhere, including `DateField` (currently hardcoded `4.dp`, the one straggler called
out directly) and the new calendar dialog. The confirm button is an exception — full pill via
`RoundedCornerShape(percent = 50)`, not a fixed radius, per §7.

## 6. Component policy — background XOR border, zero shadow

No element gets both a background fill and a border by default; which one applies is decided per
component *type*, not per instance. No shadows anywhere (confirmed explicitly) — hierarchy comes
from background-color contrast (Background < Surface < Accent) and typography weight, not
elevation.

| Component type | Treatment |
|---|---|
| List rows (transactions, categories, recurring rules) | Surface background, no border |
| Featured cards (Home summary, budget cards) | Accent background, no border |
| Buttons / FAB / selected chip / selected segment | Primary (or Income-green for confirm) background, no border |
| Unselected chips | Border only, no fill |
| Text fields | Border only (M3 `OutlinedTextField` default), no fill |
| Dialogs (incl. calendar) | Surface background, no border, no shadow — Android's own dialog-window dim separates it from the screen behind, not an app-drawn effect |
| Bottom nav | Surface background, no border |
| Selection rings (icon/color pickers) | Unchanged mechanism (`Modifier.selectionRing`), recolored to Primary |

## 7. Confirm button (new shared component)

New `ui/components/ConfirmButton.kt`, replacing every dialog's ad-hoc `TextButton`/`IconButton`
save action:

```kotlin
@Composable
fun ConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

- Shape: `RoundedCornerShape(percent = 50)` — a true pill regardless of height.
- Disabled (`enabled = false`): `DisabledFill` background, `DisabledContent` text, label "Confirm",
  no icon.
- Enabled: Income-green background, `OnConfirm` (mode-dependent) checkmark icon (Phosphor
  `Check`, already in use) + "Confirm" text.
- Label is always the localized string "Confirm" (new `action_confirm` string, Albanian +
  English) regardless of context — a deliberate simplification per the brief: one predictable
  "ready to submit" signal app-wide instead of per-screen wording ("Save"/"Add"/"Import").

**Replaces, in every case that currently gates a save action on form validity or otherwise acts
as the primary dialog action:**
- `QuickAddCategoryDialog` (`AddEditTransactionScreen.kt`) — "Save" → `ConfirmButton`.
- `AddEditCategoryDialog` (`CategoriesScreen.kt`) — "Save" → `ConfirmButton`.
- `SetBudgetDialog` (`BudgetsScreen.kt`) — "Save" → `ConfirmButton`.
- Backup import-confirm dialog (`SettingsScreen.kt`) — "Import" → `ConfirmButton` (always enabled;
  simply always renders in its green state since there's no validity gate here).
- **`AddEditTransactionScreen`** and **`AddEditRecurringRuleScreen`** — these are full-screen
  forms today, not dialogs, with a `TopAppBar` checkmark `IconButton` gated on `canSave`. Per your
  explicit call-out of "add transaction" as a target, both move to: `TopAppBar` keeps only the
  back-arrow `IconButton`; a full-width `ConfirmButton` is bottom-anchored (e.g. in `Scaffold`'s
  `bottomBar` slot) below the form content. Bigger layout change to these two screens than a pure
  restyle, but fully consistent with every dialog's confirm affordance.

`GlassButton`/`GlassDialog`'s existing `TextButton` "Cancel"/"Clear"/"Apply" actions are
unaffected — this only replaces the primary/affirmative action.

## 8. Custom calendar dialog (replaces `DatePickerDialog`)

`DateField` currently opens M3's stock `DatePickerDialog`/`DatePicker` — unstyled, doesn't match
the app's rounded/flat language. New `ui/components/CalendarDialog.kt`, built from `java.time` +
Compose (no new dependency):

- **Header:** "Month Year" (e.g. "August 2026", locale-formatted) with prev/next `IconButton`s
  (Phosphor `CaretLeft`/`CaretRight`).
- **Day-of-week row:** locale-aware, via `WeekFields.of(locale).firstDayOfWeek` — matches the
  app's existing locale-aware date handling (`rememberDateFormat`, `currentAppLocale()`), so
  Albanian locale starts the week on Monday like the rest of the app already assumes.
- **Day grid:** 6×7, `LocalDate`-driven. Days outside the current month shown dimmed
  (`OnSurfaceMuted`) but still tappable (jumps month). Selected day: Primary-filled circle,
  `OnPrimary` text. Today (if not selected): Primary-colored text, no fill, distinguishing it from
  an ordinary day without competing with the selected-day fill.
- **Footer:** `TextButton` "Cancel" + `ConfirmButton` (always enabled — a date is always selected,
  defaulting to the field's current value).
- **Container:** `Surface` background, `Radius.large` (20dp) corners, no border, no shadow —
  identical policy to every other dialog.

## 9. Rollout process

Same proven rhythm as the glassmorphism pass: shared components and tokens first (color, type,
radius, `ConfirmButton`, `CalendarDialog`), then screens sequentially, reporting after each. Order:
Home → Add/Edit Transaction → Transactions → Categories → Budgets → Reports → Recurring Rules →
Settings — same order as before, since dependencies between shared components and screens are
identical in shape.

Given the glassmorphism components are being actively deleted/shrunk (not just recolored), this
pass touches the same files as before but with **larger diffs per file** — expect more full-file
rewrites than incremental edits.

## 10. Testing & verification

Unchanged from the prior pass: no `adb`/emulator available — verification per task is
`gradlew compileDebugKotlin` plus unit tests where there's pure logic (contrast/color math isn't
independently unit-testable in a meaningful way, but the calendar's month-grid date logic and the
`ConfirmButton`'s enabled/disabled branching are). You'll need to eyeball each screen on-device
between check-ins, same as before — light AND dark mode this time, since both are now real,
distinct themes rather than dark-only with a placeholder light fallback.

## 11. Risks

- **Confirm-button text color is mode-dependent, not a constant** — easy to get backwards
  (white-on-light-fill or black-on-dark-fill) if implemented as a single fixed color; must be
  derived per-theme, not hardcoded.
- **Calendar dialog is new, non-trivial UI** — month-grid layout, leap years, locale-aware
  week-start, and the "tap a dimmed adjacent-month day to jump months" interaction all need
  correctness, unlike restyling an existing component.
- **Full-screen form layout change** (§7) — moving the confirm action from top bar to a bottom bar
  changes these two screens' structure, not just their colors; worth extra care that scrolling
  content doesn't get hidden behind the new bottom bar.
