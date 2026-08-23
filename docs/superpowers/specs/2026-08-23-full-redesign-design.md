# Full Visual Redesign — Design System & Screen-by-Screen Rework

Status: approved in chat 2026-08-23. Implementing now.

## 1. Goals

Top-to-bottom visual/UX pass over the existing app (Home, Add/Edit Transaction, Transactions,
Categories, Budgets, Reports, Recurring Rules, Settings). Formalize the partial design system
already in place (glass tokens, shape scale, typography scale from the 2026-08-22 design pass)
into a complete token set, apply glassmorphism consistently everywhere (varied by elevation, not
uniform), swap to one icon family, and add the micro-interactions the app currently has none of.

No ViewModel, Room/DAO, or query logic changes — this is a presentation-layer pass, same as the
prior design pass.

## 2. Out of scope

- No new nav structure (4-item bottom nav from the 2026-08-22 pass stays: Home | Transactions |
  Reports | Settings).
- No dependencies beyond the one icon library (below).
- No light theme (still v2 per `app/CLAUDE.md`).
- No further expansion of `CategoryColorPalette` — already expanded in a separate, already-merged
  pass (commit `94d1d42`).
- No real backdrop blur (`RenderEffect`/Haze-style content blur). The app's glassmorphism is, and
  stays, the layered-transparency technique already proven in `GlassCard` (translucent fill +
  gradient border + soft shadow + glow patch) — just varied by tier. True blur-of-content-behind
  would need a new dependency (Haze) or API 31+-only `RenderEffect` chaining across arbitrary
  content; neither is worth it for a dependency-free technique that already reads as "glass."

## 3. Design tokens (`ui/theme/`)

### 3.1 Color — new additions to `Color.kt`

```
Warning = 0xFFD9A441          // amber/ochre — distinct from Accent (gold) and Expense (terracotta)
WarningContainer = 0xFF362B18
OnWarning = 0xFF1B140A
```

Budgets get three states: fine (`Income`-tinted, <80% spent), warning (`Warning`-tinted, 80–99%),
over (`Expense`-tinted, ≥100%). Replaces today's binary Income/Expense-only progress coloring in
`BudgetsScreen`.

### 3.2 Spacing scale — new `Spacing` object

```kotlin
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}
```

Sweep raw `Spacer(Modifier.height(N.dp))` calls across all screens onto the nearest scale value.
Not a strict rename of every literal — only where a raw `dp` is standing in for spacing between
elements (not sizing a specific component like the FAB or a chart canvas).

### 3.3 Radius — expose raw scale values alongside `MaterialTheme.shapes`

New `Radius` object with the same five values already backing `FinanceFlowShapes` in `Theme.kt`
(`extraSmall=12dp, small=16dp, medium=24dp, large=28dp`, `extraLarge` same as `large`), so
asymmetric shapes (bottom nav's top-only corners, currently a bare `RoundedCornerShape(24.dp,
24.dp, 0.dp, 0.dp)` literal) can reference `Radius.medium` instead of repeating the number.
`DateField`'s stray `RoundedCornerShape(4.dp)` moves to `Radius.extraSmall` (12dp) — the one
genuine straggler found in the audit.

### 3.4 Alpha — consolidate four ad-hoc values into named tokens

Today: `0.30f` (icon/color-swatch selection, two call sites), `0.25f` (swipe-to-delete
background), `0.22f` (category row swatch background), `0.15f` (budget progress track). These are
all doing one of three jobs — collapse to:

```kotlin
object GlassAlpha {
    val containerTint = 0.20f   // swatch/icon backgrounds at rest (was 0.22f)
    val selectedTint = 0.30f    // selection emphasis (icon picker, color picker) — unchanged
    val trackTint = 0.15f       // progress-bar tracks — unchanged
    val destructiveTint = 0.25f // swipe-to-delete reveal — unchanged
}
```

(Values mostly unchanged from today's — the fix is naming them once and referencing the name at
every call site, not re-tuning them.)

### 3.5 Glass elevation tiers — replaces the single `GlassCard` tuning

Four tiers, same layered technique, scaled fill/glow/border/shadow. L2 is today's `GlassCard`
defaults, unchanged. L1 is new and intentionally the most restrained tier in the whole system —
list rows carry financial figures that must stay scannable, so L1 opacity is capped low by design,
not as an oversight.

| Tier | Used for | Fill | Border | Glow blur | Shadow |
|---|---|---|---|---|---|
| **L1 Row** | Transaction/category/recurring-rule rows | `0x14FFFFFF` (~8%) | 1dp solid `0x33FFFFFF` | none | none |
| **L2 Card** | Summary card, budget cards, settings nav rows | `0x24FFFFFF` (today's `GlassFill`) | gradient `0x5C→0x0D` | 24dp | 20dp @ `0x73` |
| **L3 Interactive** | Buttons, chips, FAB | `0x2EFFFFFF` | gradient `0x70→0x0D` | 24dp | 20dp @ `0x73` |
| **L4 Overlay** | Dialogs (incl. the date-range picker, brought into the system) | `0x30FFFFFF` | gradient `0x80→0x14` | 32dp | 28dp @ `0x85` |

Exact hex values are starting points tunable during the per-screen pass — you'll be looking at
each screen on-device as we go, per the rollout process below.

## 4. Shared component changes (`ui/components/`)

- **`GlassCard`** gains a `tier: GlassTier = GlassTier.Card` param (enum `Row/Card/Interactive/
  Overlay`) driving the table above, instead of hardcoded L2-only values. Existing call sites
  default to `Card` (no behavior change) unless reassigned below.
- **New `GlassRow`** — thin wrapper at `GlassTier.Row`, no shadow/glow, for `TransactionRow`,
  `CategoriesScreen`'s row, `RecurringRulesScreen`'s row.
- **`GlassButton`, `GlassFab`, `GlassFilterChip`** — retuned to `GlassTier.Interactive` (L3)
  values instead of their current bespoke numbers.
- **`GlassDialog`** — retuned to `GlassTier.Overlay` (L4). `TransactionsScreen`'s
  `DateRangePickerDialog` (currently a plain M3 `Surface` — the one surface outside the glass
  system entirely) moves onto `GlassDialog`.
- **New `GlassTextField`** — wraps M3 `OutlinedTextField` with the L2/L3 glass treatment (amount,
  note, label fields need strong contrast, so this sits at Card/Interactive brightness, not Row).
  Replaces the flat `OutlinedTextField`s in `AddEditTransactionScreen`,
  `AddEditRecurringRuleScreen`, and the search field in `TransactionsScreen`. `DateField` is
  restyled to match (same border/fill language) without changing its `DatePickerDialog` trigger
  behavior.
- **New `GlassSegmentedControl`** — wraps M3 `SingleChoiceSegmentedButtonRow`/`SegmentedButton`
  with glass-tinted selected/unselected states and an animated selected-segment background
  crossfade (replaces the checkmark icon removed in commit `eda2a00`). Used for every segmented
  row in the app: `TransactionTypeToggle`, the type/income-expense/currency/frequency rows in
  `AddEditTransactionScreen`/`AddEditRecurringRuleScreen`, the Overview/Budgets tab and period row
  in `ReportsScreen`, and the language/currency rows in `SettingsScreen`.
- **New selection-ring treatment** for icon-picker and color-picker grids (`CategoriesScreen`,
  `AddEditTransactionScreen`'s quick-add dialog): a border always present in the layout at 0 alpha,
  animated to full color+alpha on selection — replaces today's 0dp→3dp border-width swap, which is
  the actual source of the "layout shift on selection" the brief calls out, since it changes drawn
  border width per-frame rather than presence/absence of a fixed-width border.
- **New `Modifier.glassPressable()`** — spring-based scale-down (~0.97) on press via
  `interactionSource`, applied inside `GlassButton`, `GlassFab`, `GlassFilterChip`,
  `GlassSegmentedControl`, and `GlassRow`'s clickable variant, so every glass surface gets
  consistent tactile feedback without per-screen wiring.

## 5. Icon migration

Switch every `Icons.Rounded.*` reference (confirmed: 25 distinct glyphs across nav, category
catalog, and screen actions — no `Filled`/`Outlined` stragglers) to **Phosphor Icons**, Light/
Regular weight, via `com.adamglin:phosphor-icon-android:1.0.0` (verified current release on Maven
Central; Compose-native Android AAR).

- Map each glyph 1:1 by meaning at the point each screen is touched (not a single big-bang
  rename) — e.g. `Icons.Rounded.AttachMoney` → Phosphor `Money`/`CurrencyDollar`. If a specific
  glyph has no reasonable Phosphor equivalent, keep the Material Rounded glyph for that one icon
  and note it inline — no blocking the whole migration on an edge case.
- **Selected-state rule:** bottom nav and any tab/segment icon uses **Fill** weight when
  selected, **Regular/Light** when not — a zero-layout-shift signal layered on top of the existing
  color change, using a weight the same library already ships.

## 6. Micro-interactions & animation

The app currently has zero animation code (no `AnimatedVisibility`, `animateFloatAsState`,
`Crossfade`, or `updateTransition` anywhere) — all of the below is new, using only stdlib Compose
animation APIs.

- **Press states** — via `glassPressable()` (§4), everywhere it's applied.
- **Selection transitions** — via the selection-ring treatment (§4), ~150–200ms color/alpha
  animation.
- **Personal/Business/Combined toggle** — `GlassSegmentedControl`'s own crossfade (§4) for the
  control itself, plus `AnimatedContent`/`Crossfade` on the content that reacts to the toggle:
  Home's summary numbers, the Transactions list, and Reports' charts.
- **Reports chart entrance** — `CategoryPieChart` (raw `Canvas`/`drawArc`) animates sweep angle
  0→final on first composition via `Animatable`, staggered per category slice for a cascading
  reveal. `ReportBarChart` (Vico) gets an equivalent fade/rise-in on first composition.
- **Save/delete feedback** — a glass-styled M3 `Snackbar` ("Saved"/"Deleted") via `SnackbarHost`,
  M3 stdlib, no new dependency. Not present anywhere in the app today.

## 7. Screen-by-screen scope

Implemented in this order, foundation (§3–§6 above) first, then sequentially with a short summary
after each screen so issues get caught early:

| Screen | Current gaps (from audit) | Planned treatment |
|---|---|---|
| **Home** | Single `GlassCard`, flat `TransactionRow` list, instant toggle | `GlassRow` for recent-transaction rows, animated toggle content, `glassPressable` FAB |
| **Add/Edit Transaction** | Flat `OutlinedTextField`s, default M3 segmented rows, hardcoded selection alpha in quick-add dialog | `GlassTextField`, `GlassSegmentedControl`, selection-ring in quick-add category grid |
| **Transactions** | Flat search field, default `ExposedDropdownMenuBox`, date-range dialog outside glass system entirely | `GlassTextField` search, `GlassDialog` date-range picker, `GlassRow` list |
| **Categories** | Flat `CategoryRow`, hardcoded swatch/selection alphas, `SwipeToDismissBox` hardcoded delete tint | `GlassRow`, `GlassAlpha` tokens, selection-ring for icon/color grids |
| **Budgets** | Binary Income/Expense progress coloring, no warning tier | Three-state `Warning`/`Income`/`Expense` progress, `GlassSegmentedControl` in `SetBudgetDialog` |
| **Reports** | Charts not glass-wrapped, zero animation, default segmented tabs | Chart entrance animation (§6), `GlassSegmentedControl` for tabs/period row |
| **Recurring Rules** | Flat row, default M3 `Switch` | `GlassRow`, accent-tinted switch colors |
| **Settings** | Mostly `GlassCard` already; segmented language/currency rows default M3 | `GlassSegmentedControl` for language/currency |

## 8. Testing & verification

No `adb`/emulator available in this environment — I can't visually run the app. Verification per
screen is `gradlew compileDebugKotlin` (and existing lint/tests) confirming it builds, plus
careful review of the diff; you'll need to eyeball each screen on your device as we go, which is
the reason for the per-screen check-ins.

## 9. Risks

- **Money legibility** — L1's low opacity cap (§3.5) is the main defense; if any screen still
  reads as harder to scan than before once you look at it, that's a signal to drop that row back
  toward flat rather than push the tier further.
- **Icon mapping edge cases** — a handful of the 25 glyphs may not have a clean Phosphor
  equivalent; handled case-by-case per §5 rather than blocking.
