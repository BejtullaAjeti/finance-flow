# Design Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate the bottom nav to 4 items, centralize corner radius and glassmorphism tokens in the theme layer, swap the icon set to the Rounded variant, and replace ad-hoc FABs with a shared glass-styled component.

**Architecture:** Presentation-layer only — no ViewModel, DAO, or query logic changes. Theme tokens (`Theme.kt`, `Color.kt`) and one new shared component (`GlassFab`) are the only "system" files; every screen either already reads them (`GlassCard`, `MaterialTheme.shapes`) or gets a small, mechanical edit (icon import swap, FAB call-site swap, nav entry move).

**Tech Stack:** Jetpack Compose, Material3, existing `material-icons-extended` dependency (no new dependencies).

**Spec:** `docs/superpowers/specs/2026-08-22-design-pass-design.md`

## Global Constraints

- No new Gradle dependencies.
- No changes to `BudgetViewModel`, `ReportsViewModel`, `TransactionViewModel`, or any DAO/query — this is presentation-only.
- All user-facing strings continue to go through `strings.xml` / `values-en/strings.xml` (per `app/CLAUDE.MD`), but this pass adds no new user-facing text — it's pure restyling/relocation of existing labels.
- Every task must leave the project compiling (`./gradlew compileDebugKotlin`) and the existing unit test suite green (`./gradlew testDebugUnitTest`).

**Note on verification style:** this pass is UI/theming, not business logic — there's nothing here that fits a red/green unit-test cycle (a corner-radius token or an icon-family swap has no assertable behavior). Each task's "test" step is therefore a compile check plus a specific manual visual/interaction check to perform in the running app, not a fabricated unit test.

---

### Task 1: Theme shapes + glass token tuning

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt:199` (filter-chip shape)
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt:92` (categories row shape)

**Interfaces:**
- Produces: `MaterialTheme.shapes.extraSmall/small/medium/large/extraLarge` available to every screen via the existing `FinanceFlowTheme` wrapper in `MainActivity`. No new public functions.

- [ ] **Step 1: Add the `Shapes` definition to `Theme.kt`**

Add this above `FinanceFlowDarkScheme` (needs `import androidx.compose.material3.Shapes` and `import androidx.compose.foundation.shape.RoundedCornerShape` and `import androidx.compose.ui.unit.dp`):

```kotlin
private val FinanceFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

Then pass it into the `MaterialTheme(...)` call in `FinanceFlowTheme`:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = FinanceFlowShapes,
    content = content
)
```

- [ ] **Step 2: Tune the glass tokens in `Color.kt`**

Change these four values (leave `GlassBorderBottom` and `GlassShadow` as-is):

```kotlin
val Backdrop = Color(0xFF0B0E14)
...
val GlassFill = Color(0x24FFFFFF)
val GlassGlow = Color(0x33FFFFFF)
val GlassBorderTop = Color(0x5CFFFFFF)
```

- [ ] **Step 3: Replace the two hardcoded corner radii**

In `TransactionsScreen.kt`, the filter-chip `shape = RoundedCornerShape(4.dp)` becomes `shape = MaterialTheme.shapes.extraSmall` (add `import androidx.compose.material3.MaterialTheme` if not already present — it already is, used elsewhere in the file).

In `SettingsScreen.kt`, `.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))` becomes `.background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)`.

Both files can then drop their now-unused `import androidx.compose.foundation.shape.RoundedCornerShape` — check the rest of each file first; `TransactionsScreen.kt` also uses `RoundedCornerShape(16.dp)` for a dialog at line 231, so keep the import there. `SettingsScreen.kt` has no other use, so remove its import.

- [ ] **Step 4: Compile and visually verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

Run the app (see `run` skill if available, or `./gradlew installDebug` + launch manually) and check: bottom nav, Home's summary card, and Budget cards visibly read as frosted glass against a darker background than before; the Transactions search-filter chip and the Settings → Categories row now have visibly rounder corners.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/theme/Theme.kt app/src/main/java/com/example/financeflow/ui/theme/Color.kt app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt
git commit -m "Centralize corner radius via MaterialTheme.shapes; tune glass tokens"
```

---

### Task 2: Shared `GlassFab` component

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt`

**Interfaces:**
- Consumes: `GlassFill`, `GlassGlow`, `GlassBorderTop`, `GlassBorderBottom`, `GlassShadow` from `com.example.financeflow.ui.theme` (Task 1's tuned values); `MaterialTheme.shapes.large` from Task 1.
- Produces: `@Composable fun GlassFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit)` — used by Tasks 3 and 4's screen edits.

- [ ] **Step 1: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassBorderBottom
import com.example.financeflow.ui.theme.GlassBorderTop
import com.example.financeflow.ui.theme.GlassFill
import com.example.financeflow.ui.theme.GlassGlow
import com.example.financeflow.ui.theme.GlassShadow

/**
 * Shared floating action button styled to match [com.example.financeflow.ui.components.GlassCard]
 * — same fill/border/shadow tokens — instead of M3's flat default FAB. Larger than the M3 default
 * (64dp vs 56dp) for an easier thumb target.
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(elevation = 16.dp, shape = shape, ambientColor = GlassShadow, spotColor = GlassShadow)
            .clip(shape)
            .background(GlassGlow)
            .background(GlassFill)
            .border(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)), shape)
            .clip(shape)
            .let { base ->
                androidx.compose.foundation.clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ).let { clickableModifier -> base.then(clickableModifier) }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = { icon() }
    )
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the nested `clickable`/`remember` composition inside `.let` doesn't compile cleanly (it's an unusual shape for a `Modifier` chain), simplify to a plain sequential chain ending in `.clickable(onClick = onClick)` — the important, non-negotiable part is the visual layer order (glow → fill → border → clip), not this specific chaining style.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt
git commit -m "Add shared GlassFab component"
```

---

### Task 3: Icon set swap (Filled → Rounded)

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowDestination.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/CategoryStyle.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/DateField.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditRecurringRuleScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt`

**Interfaces:** None — this is a mechanical rename with no new symbols. Every `import androidx.compose.material.icons.filled.X` becomes `import androidx.compose.material.icons.rounded.X`, and every `Icons.Filled.X` reference becomes `Icons.Rounded.X`, for every glyph currently used (`Home`, `ReceiptLong`, `Savings`, `BarChart`, `Autorenew`, `Settings`, `AttachMoney`, `Category`, `Coffee`, `DirectionsCar`, `Fastfood`, `FitnessCenter`, `Flight`, `LocalHospital`, `Movie`, `Pets`, `Receipt`, `School`, `ShoppingCart`, `Work`, `DateRange`, `ArrowBack`, `Check`, `Search`, `Warning`, `Add`, `Delete`, `ChevronRight`).

- [ ] **Step 1: Do the rename**

For each file listed above, change every `androidx.compose.material.icons.filled.*` import to the matching `androidx.compose.material.icons.rounded.*` import, and every `Icons.Filled.X` usage to `Icons.Rounded.X`. Do not change the glyph name itself (e.g. `Icons.Filled.Home` → `Icons.Rounded.Home`, not a different icon) — this preserves meaning, only changes the terminal style.

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If any glyph has no `rounded` variant in `material-icons-extended` (rare, but a few exotic icons are filled-only), the compiler error will name the missing symbol — for that one glyph only, keep the `filled` import/usage and note it as an exception in the commit message.

- [ ] **Step 3: Visually verify**

Run the app and check the bottom nav icons, category icons (Categories screen + budget cards), and the add/edit screens' back/check/search/date icons all render with visibly rounded terminals.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui
git commit -m "Swap icon set from Filled to Rounded across the app"
```

---

### Task 4: Bottom nav consolidation (Budgets → Reports tab, Recurring/Categories → Settings) + GlassFab rollout

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowDestination.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowApp.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt` (FAB swap)
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt` (FAB swap)
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt` (FAB swap)
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt` (FAB added)
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` (one new string: `settings_recurring`, plus two new tab labels `report_tab_overview` / `report_tab_budgets`)

**Interfaces:**
- Consumes: `GlassFab` from Task 2; `MaterialTheme.shapes` from Task 1.
- Produces: `FinanceFlowDestination.entries` now has exactly 4 entries (`Home, Transactions, Reports, Settings`); `CATEGORIES_ROUTE`-style routing to `"recurring"` stays available for Settings to navigate to directly (the route string itself, `"recurring"`, is unchanged — only what points to it changes).

- [ ] **Step 1: Trim `FinanceFlowDestination`**

Remove the `Budgets` and `Recurring` enum entries and their now-unused icon imports (`Savings`, `Autorenew` — after Task 3 these are `Icons.Rounded.Savings`/`Icons.Rounded.Autorenew`; drop those two imports along with the entries). Result:

```kotlin
enum class FinanceFlowDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Home("home", R.string.nav_home, Icons.Rounded.Home),
    Transactions("transactions", R.string.nav_transactions, Icons.Rounded.ReceiptLong),
    Reports("reports", R.string.nav_reports, Icons.Rounded.BarChart),
    Settings("settings", R.string.nav_settings, Icons.Rounded.Settings)
}
```

Leave `"budgets"` and `"recurring"` as plain string route constants so `FinanceFlowApp`'s `NavHost` can still register them:

```kotlin
const val BUDGETS_ROUTE = "budgets"
const val RECURRING_ROUTE = "recurring"
```

- [ ] **Step 2: Update `FinanceFlowApp`'s `NavHost`**

Change `composable(FinanceFlowDestination.Budgets.route) { BudgetsScreen() }` to `composable(BUDGETS_ROUTE) { BudgetsScreen() }`, and `composable(FinanceFlowDestination.Recurring.route) { RecurringRulesScreen(...) }` to `composable(RECURRING_ROUTE) { RecurringRulesScreen(...) }` (keep the existing `onAddRule`/`onEditRule` lambdas as-is). Add navigation from Settings to `RECURRING_ROUTE`: `SettingsScreen(onNavigateToCategories = ..., onNavigateToRecurring = { navController.navigate(RECURRING_ROUTE) })`.

- [ ] **Step 3: Add the Overview/Budgets tab to `ReportsScreen`**

Add two string resources (`values/strings.xml` Albanian text your call to match existing tone, `values-en/strings.xml`):

```xml
<string name="report_tab_overview">Përmbledhje</string>
<string name="report_tab_budgets">Buxhetet</string>
```
```xml
<string name="report_tab_overview">Overview</string>
<string name="report_tab_budgets">Budgets</string>
```

In `ReportsScreen.kt`, wrap the existing body in a tab switch. Add `var selectedTab by remember { mutableStateOf(0) }` and, right after the function signature's `val` declarations, a `SingleChoiceSegmentedButtonRow` with two `SegmentedButton`s (`report_tab_overview` index 0, `report_tab_budgets` index 1 — same `SegmentedButtonDefaults.itemShape(index, 2)` pattern already used for `ReportPeriod`). When `selectedTab == 0`, render the existing body (period selector through `CategoryPieChart`) unchanged. When `selectedTab == 1`, render `BudgetsScreen()` (add the import `com.example.financeflow.ui.screens.BudgetsScreen` — same package, no import needed actually since it's already in `com.example.financeflow.ui.screens`). Keep `ReportsScreen`'s existing `.verticalScroll(rememberScrollState())` Column wrapping only the tab-0 content, since `BudgetsScreen` has its own `LazyColumn` (nesting a scrollable `LazyColumn` inside a `verticalScroll` Column crashes) — structure it as: outer `Column` (no scroll) containing the tab row, then a conditional that is either the existing scrollable inner `Column` (tab 0) or `BudgetsScreen()` directly (tab 1).

- [ ] **Step 4: Add the Recurring row to `SettingsScreen`**

Add a `onNavigateToRecurring: () -> Unit` parameter to `SettingsScreen`'s signature (alongside the existing `onNavigateToCategories`). Add a new string `settings_recurring` (both languages — Albanian and `"Recurring"` for English) and duplicate the existing Categories `Row` block right after it, pointing at `onNavigateToRecurring`:

```kotlin
Spacer(Modifier.height(12.dp))

Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
        .clickable(onClick = onNavigateToRecurring)
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text(text = stringResource(R.string.settings_recurring), style = MaterialTheme.typography.bodyLarge)
    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
}
```

- [ ] **Step 5: Swap the three existing FABs to `GlassFab`**

In `HomeScreen.kt`, `RecurringRulesScreen.kt`, `CategoriesScreen.kt`: replace `FloatingActionButton(onClick = X) { Icon(Icons.Rounded.Add, contentDescription = Y) }` with `GlassFab(onClick = X, contentDescription = Y) { Icon(Icons.Rounded.Add, contentDescription = null) }` (the outer `contentDescription` param carries the semantics now, so the inner `Icon`'s stays `null` to avoid double-announcing to accessibility services). Remove the now-unused `FloatingActionButton` import from each; add `import com.example.financeflow.ui.components.GlassFab`.

- [ ] **Step 6: Add a FAB to `TransactionsScreen`**

`TransactionsScreen` currently has no `Scaffold`/FAB — it's a bare `Column`. Wrap its existing content in a `Scaffold(floatingActionButton = { GlassFab(onClick = onAddTransaction, contentDescription = stringResource(R.string.home_add_transaction_content_description)) { Icon(Icons.Rounded.Add, contentDescription = null) } })`, moving the current `Column(...)` into the Scaffold's content lambda (apply `innerPadding` the same way `HomeScreen` does: `.padding(innerPadding).padding(16.dp)`, replacing the existing bare `.padding(16.dp)`). This requires adding an `onAddTransaction: () -> Unit` parameter to `TransactionsScreen`'s signature and threading it through from `FinanceFlowApp`'s `composable(FinanceFlowDestination.Transactions.route) { TransactionsScreen(onEditTransaction = ..., onAddTransaction = { navController.navigate("transaction") }) }` (same route Home already navigates to for adding).

- [ ] **Step 7: Compile and manually verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

Run the app and check: bottom nav shows exactly 4 items; Reports has a working Overview/Budgets tab switch that shows the same budget cards the old Budgets tab showed; Settings shows both Categories and Recurring rows navigating correctly; Home, Transactions, Recurring, and Categories all show the new glass-styled FAB in a consistent bottom-right position.

- [ ] **Step 8: Run the full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests (`ConvertersTest`, `RecurringRuleGenerationTest`, `ReportPeriodRangeTest`, `BackupSerializerTest`, `BudgetViewModelTest`, `CategoryViewModelTest`) still pass unchanged — none of this task's changes touch the code any of them exercise.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "Consolidate bottom nav to 4 items; roll out GlassFab to Home/Transactions/Recurring/Categories"
```

---

## Self-Review Notes

- **Spec coverage:** §1 nav → Task 4; §2 shapes → Task 1; §3 glass tokens → Task 1; §4 icons → Task 3; §5 FAB → Tasks 2 & 4. All five spec sections covered.
- **Ordering rationale:** Theme tokens first (Task 1) so later tasks can reference `MaterialTheme.shapes` and the tuned glass colors; `GlassFab` (Task 2) before its rollout (Task 4) so the component exists when referenced; icon swap (Task 3) is independent of nav/FAB work and can run any time after Task 1, placed before Task 4 only so Task 4's code samples can use `Icons.Rounded.*` directly instead of `Icons.Filled.*`.
