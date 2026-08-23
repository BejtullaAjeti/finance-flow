# Design Pass — Navigation, Shapes, Glass, Icons, FAB

Status: approved in chat 2026-08-22. Implementing now.

## 1. Navigation consolidation

Bottom nav goes from 6 items to 4: `Home | Transactions | Reports | Settings`.

- `ReportsScreen` gains an internal `SingleChoiceSegmentedButtonRow` tab: **Overview** (existing
  charts/breakdown) | **Budgets** (existing `BudgetsScreen` composable, re-hosted unchanged — its
  `BudgetViewModel`/`BudgetCard` logic is not rewritten).
- `SettingsScreen` gains a **Recurring** row identical in style to the existing **Categories** row
  (same `Row` + `background` + `clickable` + `ChevronRight` pattern), navigating to the existing
  `RecurringRulesScreen` route.
- `FinanceFlowDestination` enum drops `Budgets` and `Recurring` entries. Their routes stay
  reachable via `CATEGORIES_ROUTE`-style constants, navigated to from Settings/Reports instead of
  the bottom nav.
- `FinanceFlowApp`'s `NavHost` keeps `composable(...)` entries for `budgets`/`recurring` routes;
  only which UI surfaces the route changes.

## 2. Shared corner radius

`Theme.kt` adds an explicit `Shapes(...)` passed into `MaterialTheme(shapes = ...)`, sized off
`GlassCard`'s existing 24dp default:

| Token | Radius |
|---|---|
| extraSmall | 12dp |
| small | 16dp |
| medium | 24dp |
| large | 28dp |
| extraLarge | 28dp |

`GlassCard`'s own `cornerRadius: Dp = 24.dp` default is unchanged (it already matches `medium`);
it keeps its own explicit parameter since callers (e.g. the bottom nav's top-corners-only shape)
override it directly. The two hardcoded outliers become theme-driven:
- `TransactionsScreen`'s filter-chip `RoundedCornerShape(4.dp)` → `MaterialTheme.shapes.extraSmall`
- `SettingsScreen`'s row `RoundedCornerShape(8.dp)` → `MaterialTheme.shapes.small`

## 3. Glass token tuning

In `Color.kt`, raise alpha so the frosted layering reads clearly, and richen the backdrop:

- `Backdrop`: `0xFF11151C` → `0xFF0B0E14` (darker, so cards have more contrast to float against)
- `GlassFill`: `0x0FFFFFFF` → `0x24FFFFFF`
- `GlassGlow`: `0x1AFFFFFF` → `0x33FFFFFF`
- `GlassBorderTop`: `0x47FFFFFF` → `0x5CFFFFFF`
- `GlassBorderBottom` and `GlassShadow` unchanged.

No component code changes — `GlassCard` already reads these tokens, so every screen picks this up
for free.

## 4. Icon set

Replace `androidx.compose.material.icons.filled.*` imports and `Icons.Filled.X` references with
the `rounded` package/variant (`androidx.compose.material.icons.rounded.*`, `Icons.Rounded.X`)
across: `FinanceFlowDestination`, `CategoryStyle` (`CategoryIcons.Catalog` + `Fallback`), and every
screen file that imports `Icons.Filled.*`. Same glyphs, rounded terminals. No dependency change —
`material-icons-extended` (already in `libs.versions.toml`) ships all variants.

## 5. Shared FAB

New `GlassFab` composable in `ui/components/GlassFab.kt`: wraps M3 `FloatingActionButton` with
the glass tokens (translucent fill via `GlassFill`, gradient border via `GlassBorderTop`/
`GlassBorderBottom`, shadow/glow via `GlassShadow`/`GlassGlow`, same visual language as
`GlassCard`), sized 64dp (up from M3's default 56dp), shape `MaterialTheme.shapes.large`.

Replaces the three existing `FloatingActionButton` call sites:
- `HomeScreen` (add transaction)
- `RecurringRulesScreen` (add rule)
- `CategoriesScreen` (add category)

And is added to `TransactionsScreen`, which currently has no add-transaction affordance of its
own (relies on Home's FAB only) — navigates to the same `AddEditTransactionScreen` route Home uses.

## Out of scope

- No changes to `BudgetViewModel`, `ReportsViewModel`, or any query/aggregation logic — this is a
  presentation-layer pass only.
- No new dependencies.
- Multi-currency support and the category-seeding fix are separate, already-completed/separate
  spec cycles.
