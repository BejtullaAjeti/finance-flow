# Full Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Formalize the app's partial design system into a complete token set (color, spacing,
radius, alpha, glass elevation tiers) and apply it consistently across every screen and shared
component, replacing the icon family and adding the micro-interactions the app currently has none
of — a presentation-layer pass only.

**Architecture:** Foundation-first: token objects and shared components (Tasks 1–10) land before
any screen is touched, so no screen gets redone once a later screen reveals a missing piece.
Screens (Tasks 11–18) are then swept sequentially in the order listed, each producing an
independently reviewable, compiling diff.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (Compose BOM 2026.02.01), Material3, unchanged.
One new dependency: `com.adamglin:phosphor-icon-android:1.0.0`.

**Spec:** `docs/superpowers/specs/2026-08-23-full-redesign-design.md`

## Global Constraints

- Min SDK 26, Kotlin 2.2.10, Compose BOM 2026.02.01 — unchanged, do not bump.
- No new dependencies beyond `com.adamglin:phosphor-icon-android:1.0.0`.
- No ViewModel, Room/DAO, or query logic changes anywhere in this plan.
- No nav structure changes — the 4-item bottom nav (Home | Transactions | Reports | Settings)
  stays as-is.
- No light theme work (`FinanceFlowLightScheme` stays a non-crashing fallback only).
- No real backdrop blur / no Haze-style dependency — glass stays the layered-transparency
  technique (translucent fill + gradient border + shadow + glow patch) already in `GlassCard`,
  parameterized by tier.
- No `adb`/emulator available to the implementer — verification per task is
  `gradlew compileDebugKotlin` (and unit tests where the task has pure logic) plus code review.
  Visual correctness is confirmed by the user on-device between screen tasks, not by the
  implementer.

---

## Task 1: Design tokens — GlassTier, Spacing, Radius, GlassAlpha, Warning color

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/theme/GlassTier.kt`
- Create: `app/src/main/java/com/example/financeflow/ui/theme/Spacing.kt`
- Create: `app/src/main/java/com/example/financeflow/ui/theme/Radius.kt`
- Create: `app/src/main/java/com/example/financeflow/ui/theme/GlassAlpha.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/theme/Color.kt`
- Test: `app/src/test/java/com/example/financeflow/ui/theme/GlassTierTest.kt`

**Interfaces:**
- Consumes: existing `GlassFill`, `GlassGlow`, `GlassBorderTop`, `GlassBorderBottom`, `GlassShadow`
  from `Color.kt` (unchanged, reused as the `Card` tier's values).
- Produces:
  - `enum class GlassTier { Row, Card, Interactive, Overlay }`
  - `data class GlassTierSpec(val fill: Color, val borderBrush: Brush, val glowBlur: Dp?, val glowColor: Color?, val shadowElevation: Dp, val shadowColor: Color)`
  - `fun GlassTier.spec(): GlassTierSpec`
  - `object Spacing { val xs, sm, md, lg, xl, xxl, xxxl: Dp }` (4/8/12/16/20/24/32dp)
  - `object Radius { val extraSmall, small, medium, large, extraLarge: Dp }` (12/16/24/28/28dp)
  - `object GlassAlpha { val containerTint, selectedTint, trackTint, destructiveTint: Float }`
    (0.20f/0.30f/0.15f/0.25f)
  - `Color.kt` additions: `val Warning = Color(0xFFD9A441)`, `val WarningContainer = Color(0xFF362B18)`, `val OnWarning = Color(0xFF1B140A)`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.financeflow.ui.theme

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTierTest {
    @Test
    fun `fill opacity increases with tier from Row to Overlay`() {
        val row = GlassTier.Row.spec().fill.alpha
        val card = GlassTier.Card.spec().fill.alpha
        val interactive = GlassTier.Interactive.spec().fill.alpha
        val overlay = GlassTier.Overlay.spec().fill.alpha

        assertTrue("Row ($row) should be more transparent than Card ($card)", row < card)
        assertTrue("Card ($card) should be no more opaque than Interactive ($interactive)", card <= interactive)
        assertTrue("Interactive ($interactive) should be more transparent than Overlay ($overlay)", interactive < overlay)
    }

    @Test
    fun `Row tier has no glow or shadow`() {
        val row = GlassTier.Row.spec()
        assertTrue(row.glowBlur == null)
        assertTrue(row.shadowElevation.value == 0f)
    }

    @Test
    fun `Warning is distinct from Expense and Accent`() {
        assertNotEquals(Warning, Expense)
        assertNotEquals(Warning, Accent)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.theme.GlassTierTest"`
Expected: FAIL (compile error — `GlassTier` does not exist yet)

- [ ] **Step 3: Write the token files**

`ui/theme/GlassTier.kt`:

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTier { Row, Card, Interactive, Overlay }

data class GlassTierSpec(
    val fill: Color,
    val borderBrush: Brush,
    val glowBlur: Dp?,
    val glowColor: Color?,
    val shadowElevation: Dp,
    val shadowColor: Color
)

fun GlassTier.spec(): GlassTierSpec = when (this) {
    GlassTier.Row -> GlassTierSpec(
        fill = Color(0x14FFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))),
        glowBlur = null,
        glowColor = null,
        shadowElevation = 0.dp,
        shadowColor = Color.Transparent
    )
    GlassTier.Card -> GlassTierSpec(
        fill = GlassFill,
        borderBrush = Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)),
        glowBlur = 24.dp,
        glowColor = GlassGlow,
        shadowElevation = 20.dp,
        shadowColor = GlassShadow
    )
    GlassTier.Interactive -> GlassTierSpec(
        fill = Color(0x2EFFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x70FFFFFF), GlassBorderBottom)),
        glowBlur = 24.dp,
        glowColor = GlassGlow,
        shadowElevation = 20.dp,
        shadowColor = GlassShadow
    )
    GlassTier.Overlay -> GlassTierSpec(
        fill = Color(0x30FFFFFF),
        borderBrush = Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x14FFFFFF))),
        glowBlur = 32.dp,
        glowColor = Color(0x40FFFFFF),
        shadowElevation = 28.dp,
        shadowColor = Color(0x85000000)
    )
}
```

`ui/theme/Spacing.kt`:

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.ui.unit.dp

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

`ui/theme/Radius.kt`:

```kotlin
package com.example.financeflow.ui.theme

import androidx.compose.ui.unit.dp

object Radius {
    val extraSmall = 12.dp
    val small = 16.dp
    val medium = 24.dp
    val large = 28.dp
    val extraLarge = 28.dp
}
```

`ui/theme/GlassAlpha.kt`:

```kotlin
package com.example.financeflow.ui.theme

object GlassAlpha {
    const val containerTint = 0.20f
    const val selectedTint = 0.30f
    const val trackTint = 0.15f
    const val destructiveTint = 0.25f
}
```

In `Color.kt`, append after the existing glassmorphism tokens:

```kotlin
// Budget warning tier — distinct from Accent (gold) and Expense (terracotta)
val Warning = Color(0xFFD9A441)
val WarningContainer = Color(0xFF362B18)
val OnWarning = Color(0xFF1B140A)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradlew.bat testDebugUnitTest --tests "com.example.financeflow.ui.theme.GlassTierTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/theme/GlassTier.kt app/src/main/java/com/example/financeflow/ui/theme/Spacing.kt app/src/main/java/com/example/financeflow/ui/theme/Radius.kt app/src/main/java/com/example/financeflow/ui/theme/GlassAlpha.kt app/src/main/java/com/example/financeflow/ui/theme/Color.kt app/src/test/java/com/example/financeflow/ui/theme/GlassTierTest.kt
git commit -m "Add design tokens: glass elevation tiers, spacing, radius, alpha, warning color"
```

---

## Task 2: `Modifier.pressScale()` — shared press feedback

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/GlassPressable.kt`

**Interfaces:**
- Consumes: nothing new (Compose foundation/animation stdlib only).
- Produces: `fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier`
  — reads press state from a caller-supplied `InteractionSource` and animates `scaleX`/`scaleY` via
  `graphicsLayer`. Does **not** own click handling — callers wire their own `clickable(...)` /
  M3 component `interactionSource` param to the same source, so it composes with both custom
  `Box`-based components and M3 components that already own their click behavior.

- [ ] **Step 1: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassPressable.kt
git commit -m "Add Modifier.pressScale for shared glass press feedback"
```

---

## Task 3: `Modifier.selectionRing()` — zero-layout-shift selection state

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/SelectionRing.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `fun Modifier.selectionRing(selected: Boolean, shape: Shape, color: Color, width: Dp = 2.dp): Modifier`
  — always draws a border of constant `width`; only the color animates between `Color.Transparent`
  and `color`, so neither layout size nor drawn stroke width ever changes on selection.

- [ ] **Step 1: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.selectionRing(
    selected: Boolean,
    shape: Shape,
    color: Color,
    width: Dp = 2.dp
): Modifier = composed {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) color else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "selectionRing"
    )
    border(width, animatedColor, shape)
}
```

- [ ] **Step 2: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/SelectionRing.kt
git commit -m "Add Modifier.selectionRing for animated, layout-stable selection state"
```

---

## Task 4: `GlassCard` — add `tier` parameter

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassCard.kt`

**Interfaces:**
- Consumes: `GlassTier`, `GlassTier.spec()`, `GlassTierSpec` from Task 1.
- Produces: `GlassCard(modifier: Modifier = Modifier, tier: GlassTier = GlassTier.Card, cornerRadius: Dp = 24.dp, shape: Shape = RoundedCornerShape(cornerRadius), contentPadding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit)`
  — new `tier` param inserted after `modifier`; default `GlassTier.Card` reproduces today's
  values exactly (its `spec()` mirrors the current hardcoded `GlassFill`/`GlassGlow`/
  `GlassBorderTop`/`GlassBorderBottom`/`GlassShadow`/`24.dp`/`20.dp` constants), so every existing
  call site (which doesn't pass `tier`) is visually unchanged by this task.

- [ ] **Step 1: Rewrite the component body to read from `tier.spec()`**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.spec

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    cornerRadius: Dp = 24.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = tier.spec()
    Box(
        modifier = modifier
            .shadow(elevation = spec.shadowElevation, shape = shape, ambientColor = spec.shadowColor, spotColor = spec.shadowColor)
            .clip(shape)
            .background(spec.fill)
            .border(1.dp, spec.borderBrush, shape)
    ) {
        // ponytail: RenderEffect blur only renders on API 31+ (minSdk is 26); below that this
        // glow patch is simply inert, and the card still reads as glass via the translucent
        // fill + gradient border + shadow on their own.
        if (spec.glowBlur != null && spec.glowColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(contentPadding * 2)
                    .blur(spec.glowBlur)
                    .background(spec.glowColor, shape)
            )
        }
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
```

- [ ] **Step 2: Verify it compiles and every existing call site still resolves**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL — this confirms every current `GlassCard(...)` call site (Home summary
card, Budget cards, Settings nav rows, `GlassDialog`'s internal usage) still compiles unchanged.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassCard.kt
git commit -m "Parameterize GlassCard by glass elevation tier"
```

---

## Task 5: Retune GlassButton, GlassFab, GlassFilterChip, GlassDialog to elevation tiers

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassButton.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassFilterChip.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/GlassDialog.kt`

**Interfaces:**
- Consumes: `GlassTier`, `GlassTier.spec()` (Task 1), `Modifier.pressScale()` (Task 2), `GlassCard`'s
  `tier` param (Task 4).
- Produces: no new public API — same signatures as today for all four, now sourcing their
  fill/border/glow/shadow from `GlassTier.Interactive.spec()` (Button, Fab, FilterChip) or passing
  `tier = GlassTier.Overlay` to the inner `GlassCard` (Dialog), and each wiring an
  `interactionSource` through to `pressScale()`.

- [ ] **Step 1: `GlassDialog.kt`** — it already composes `GlassCard` internally per the prior
  design pass; change that call to pass `tier = GlassTier.Overlay` instead of relying on
  `GlassCard`'s default. No other changes needed in this file.

- [ ] **Step 2: `GlassButton.kt`** — replace its direct references to `GlassFill`/
  `GlassBorderTop`/`GlassBorderBottom` (or equivalent M3 `ButtonDefaults.colors(...)` overrides)
  with `GlassTier.Interactive.spec()`'s `fill`/`borderBrush`. Add a
  `val interactionSource = remember { MutableInteractionSource() }`, pass it into the wrapped M3
  `Button(interactionSource = interactionSource, ...)`, and chain
  `.pressScale(interactionSource)` on the outer `Modifier`.

- [ ] **Step 3: `GlassFilterChip.kt`** — same treatment as Step 2: source colors from
  `GlassTier.Interactive.spec()`, thread an `interactionSource` into the wrapped M3 `FilterChip`,
  chain `.pressScale(interactionSource)`.

- [ ] **Step 4: `GlassFab.kt`** — it's a custom `Box` (not wrapping an M3 component), so replace
  its hardcoded fill/border/glow/shadow literals with `GlassTier.Interactive.spec()` values. Since
  it owns its own click handling directly (custom `Box.clickable`), create a
  `val interactionSource = remember { MutableInteractionSource() }`, pass it to its own
  `clickable(interactionSource = interactionSource, indication = null, onClick = onClick)`, and
  chain `.pressScale(interactionSource)`. Keep its `64.dp` size and `MaterialTheme.shapes.large`
  shape unchanged — those aren't tier-driven.

- [ ] **Step 5: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassButton.kt app/src/main/java/com/example/financeflow/ui/components/GlassFab.kt app/src/main/java/com/example/financeflow/ui/components/GlassFilterChip.kt app/src/main/java/com/example/financeflow/ui/components/GlassDialog.kt
git commit -m "Retune interactive glass components to elevation tiers, add press feedback"
```

---

## Task 6: New `GlassRow` component (Row tier, for list rows)

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/GlassRow.kt`

**Interfaces:**
- Consumes: `GlassTier.Row.spec()` (Task 1), `Modifier.pressScale()` (Task 2), `Spacing`, `Radius`
  (Task 1).
- Produces: `@Composable fun GlassRow(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, shape: Shape = RoundedCornerShape(Radius.extraSmall), contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md), content: @Composable RowScope.() -> Unit)`

- [ ] **Step 1: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.theme.spec

@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.extraSmall),
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    content: @Composable RowScope.() -> Unit
) {
    val spec = GlassTier.Row.spec()
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(shape)
        .background(spec.fill)
        .border(1.dp, spec.borderBrush, shape)
    if (onClick != null) {
        base = base
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScale(interactionSource)
    }
    Row(modifier = base.padding(contentPadding), content = content)
}
```

(Note: `1.dp` needs `import androidx.compose.ui.unit.dp`.)

- [ ] **Step 2: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassRow.kt
git commit -m "Add GlassRow: Row-tier glass wrapper for list rows"
```

---

## Task 7: New `GlassTextField` component

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/GlassTextField.kt`

**Interfaces:**
- Consumes: `GlassTier`, `GlassTier.spec()` (Task 1).
- Produces:

```kotlin
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    tier: GlassTier = GlassTier.Card
)
```

- [ ] **Step 1: Write the component**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.spec

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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    tier: GlassTier = GlassTier.Card
) {
    val spec = tier.spec()
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
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = spec.fill,
            unfocusedContainerColor = spec.fill,
            focusedBorderColor = spec.borderBrush.let { MaterialTheme.colorScheme.primary },
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
```

(`focusedBorderColor` can't take a `Brush` — M3's `OutlinedTextFieldDefaults.colors` takes solid
`Color`s only. Use `MaterialTheme.colorScheme.primary` for the focused border, matching the app's
`Accent` token which is already wired as `primary` in `Theme.kt`, and `MaterialTheme.colorScheme.outline`
unfocused — this keeps the field readable while still visually distinct once focused. Drop the
unused `spec.borderBrush` reference from the snippet above.)

- [ ] **Step 2: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassTextField.kt
git commit -m "Add GlassTextField: glass-tinted OutlinedTextField wrapper"
```

---

## Task 8: New `GlassSegmentedControl` component

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/GlassSegmentedControl.kt`

**Interfaces:**
- Consumes: `Spacing`, `Radius` (Task 1).
- Produces:

```kotlin
@Composable
fun <T> GlassSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
)
```

This is a generic replacement for every M3 `SingleChoiceSegmentedButtonRow`/`SegmentedButton` use
in the app (`TransactionTypeToggle`, type/income-expense/currency/frequency rows, Reports tabs and
period row, Settings language/currency rows) — later screen tasks call this directly instead of
the M3 primitives.

- [ ] **Step 1: Write the component**

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.ui.theme.Radius
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.theme.spec

@Composable
fun <T> GlassSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    val outerSpec = GlassTier.Row.spec()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.small))
            .background(outerSpec.fill)
            .padding(Spacing.xs)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val segmentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.Transparent,
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
                Text(text = label(option), color = textColor, textAlign = TextAlign.Center)
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/GlassSegmentedControl.kt
git commit -m "Add GlassSegmentedControl: animated glass-tinted segmented control"
```

---

## Task 9: Shared snackbar controller for save/delete feedback

**Files:**
- Create: `app/src/main/java/com/example/financeflow/ui/components/SnackbarController.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowApp.kt`

**Interfaces:**
- Consumes: `GlassCard`, `GlassTier.Overlay` (Tasks 1, 4).
- Produces:
  - `class SnackbarController(private val hostState: SnackbarHostState, private val scope: CoroutineScope) { fun show(message: String) }`
  - `val LocalSnackbarController: ProvidableCompositionLocal<SnackbarController>` — screens read it
    via `LocalSnackbarController.current.show("Saved")` in later tasks.

- [ ] **Step 1: Write the controller**

```kotlin
package com.example.financeflow.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String) {
        scope.launch { hostState.showSnackbar(message) }
    }
}

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("No SnackbarController provided — wrap content in FinanceFlowApp's CompositionLocalProvider")
}
```

- [ ] **Step 2: Wire it into `FinanceFlowApp.kt`'s `Scaffold`**

Find the existing `Scaffold(...)` call in `FinanceFlowApp.kt` (it already hosts the bottom bar via
`GlassCard`). Add a `SnackbarHostState`, a `rememberCoroutineScope()`, build the
`SnackbarController`, and wrap the `Scaffold`'s content (or the whole `NavHost`, whichever is the
outermost composable in this file) in `CompositionLocalProvider(LocalSnackbarController provides controller)`.
Add a `snackbarHost` param to `Scaffold`:

```kotlin
snackbarHost = {
    SnackbarHost(hostState) { data ->
        GlassCard(tier = GlassTier.Overlay, contentPadding = 16.dp) {
            Text(data.visuals.message)
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/components/SnackbarController.kt app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowApp.kt
git commit -m "Add shared glass-styled snackbar controller for save/delete feedback"
```

---

## Task 10: Phosphor icon dependency + nav/category icon catalog migration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowDestination.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/CategoryStyle.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: every nav destination and category-catalog entry now resolves to a
  `PhosphorIcons.Regular.*` / `PhosphorIcons.Fill.*` `ImageVector` instead of
  `Icons.Rounded.*`. Later screen tasks (11–18) consume this same `PhosphorIcons` API for their
  own screen-local icon references, using the mapping table below.

**Icon mapping table** (Material Rounded name → Phosphor name; verify exact casing against
`PhosphorIcons.Regular.` autocomplete when editing — if a name doesn't resolve, pick the closest
semantically-equivalent glyph from the same weight object and note the substitution inline):

| Material Rounded | Phosphor |
|---|---|
| `AttachMoney` | `Money` |
| `Category` | `Tag` |
| `Coffee` | `Coffee` |
| `DirectionsCar` | `Car` |
| `Fastfood` | `ForkKnife` |
| `FitnessCenter` | `Barbell` |
| `Flight` | `Airplane` |
| `Home` | `House` |
| `LocalHospital` | `FirstAid` |
| `Movie` | `FilmSlate` |
| `Pets` | `PawPrint` |
| `Receipt` / `ReceiptLong` | `Receipt` |
| `School` | `GraduationCap` |
| `ShoppingCart` | `ShoppingCart` |
| `Work` | `Briefcase` |
| `DateRange` | `CalendarBlank` |
| `Close` | `X` |
| `BarChart` | `ChartBar` |
| `Settings` | `Gear` |
| `ArrowBack` | `ArrowLeft` |
| `Check` | `Check` |
| `Add` | `Plus` |
| `Warning` | `WarningCircle` |
| `Delete` | `Trash` |
| `ChevronRight` | `CaretRight` |
| `Search` | `MagnifyingGlass` |

- [ ] **Step 1: Add the dependency**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
phosphorIcon = "1.0.0"
```

Add to `[libraries]`:

```toml
phosphor-icon = { group = "com.adamglin", name = "phosphor-icon-android", version.ref = "phosphorIcon" }
```

In `app/build.gradle.kts`, add to the `dependencies { ... }` block (near the existing
`androidx-compose-material-icons-extended` line):

```kotlin
implementation(libs.phosphor.icon)
```

- [ ] **Step 2: Migrate `FinanceFlowDestination.kt`**

Replace each `Icons.Rounded.X` import/reference with `PhosphorIcons.Regular.Y` per the mapping
table (Home→House, ReceiptLong→Receipt, BarChart→ChartBar, Settings→Gear). Apply the
selected-state rule from the spec: each nav entry's icon composable should pick
`PhosphorIcons.Fill.Y` when that destination is the currently-selected route, and
`PhosphorIcons.Regular.Y` otherwise — this logic lives wherever `FinanceFlowBottomBar` currently
renders each `NavigationBarItem`'s icon in `FinanceFlowApp.kt` (not this file, if the icon
selection happens at the call site) — check both files and apply the Fill/Regular switch at
whichever one actually renders the `Icon(...)` composable per nav item.

- [ ] **Step 3: Migrate `CategoryStyle.kt`'s `CategoryIcons.Catalog` and `Fallback`**

Replace each of the 14 catalog entries' `Icons.Rounded.X` with `PhosphorIcons.Regular.Y` per the
table (AttachMoney→Money, Category→Tag, Coffee→Coffee, DirectionsCar→Car, Fastfood→ForkKnife,
FitnessCenter→Barbell, Flight→Airplane, Home→House, LocalHospital→FirstAid, Movie→FilmSlate,
Pets→PawPrint, Receipt→Receipt, School→GraduationCap, ShoppingCart→ShoppingCart, Work→Briefcase).
Use `PhosphorIcons.Regular.Tag` (or the closest generic glyph) for `Fallback`.

- [ ] **Step 4: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL — if any `PhosphorIcons.Regular.X`/`PhosphorIcons.Fill.X` reference is
unresolved, open the library's generated icon object (Android Studio autocomplete on
`PhosphorIcons.Regular.`) and substitute the closest matching glyph name; update the table above
to match what was actually used.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/example/financeflow/ui/navigation/FinanceFlowDestination.kt app/src/main/java/com/example/financeflow/ui/components/CategoryStyle.kt
git commit -m "Add Phosphor Icons dependency, migrate nav and category icon catalogs"
```

---

## Task 11: Home screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt`

**Interfaces:**
- Consumes: `GlassRow` (Task 6), `GlassSegmentedControl` (Task 8), `PhosphorIcons` (Task 10, for
  the `Add` icon → `Plus`), `pressScale` already wired inside `GlassFab` (Task 5).

- [ ] **Step 1: Replace the flat recent-transactions `TransactionRow` list container**

Wrap each row (or the row's existing content) in `GlassRow` instead of a plain `Row`/`Column` —
keep `TransactionRow`'s existing content composable as the `content` lambda passed to `GlassRow`,
so the money-figure styling (`Income`/`Expense`/`MoneyFigure`) is untouched.

- [ ] **Step 2: Replace `TransactionTypeToggle` usage with `GlassSegmentedControl`**

If `TransactionTypeToggle` is only used here and in a couple of other screens as a thin wrapper
around the type enum, update `TransactionTypeToggle.kt` itself to delegate to
`GlassSegmentedControl` internally (so this screen's call site doesn't need to change), i.e.:

```kotlin
@Composable
fun TransactionTypeToggle(
    selected: TransactionFilterType,
    onSelect: (TransactionFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSegmentedControl(
        options = TransactionFilterType.entries,
        selected = selected,
        onSelect = onSelect,
        label = { it.displayLabel() }, // reuse whatever label logic TransactionTypeToggle already has
        modifier = modifier
    )
}
```

(Use the actual existing enum/label function names found in `TransactionTypeToggle.kt` — this
snippet shows the delegation shape, not literal replacement text, since the exact type name wasn't
captured in the design-phase audit.)

- [ ] **Step 3: Animate content that reacts to the toggle**

Wrap the summary numbers and the recent-transactions list in `AnimatedContent(targetState = selectedType)`
(or `Crossfade(targetState = selectedType)`) so switching Personal/Business/Combined fades instead
of instantly swapping.

- [ ] **Step 4: Swap the FAB's `Add` icon reference to `PhosphorIcons.Regular.Plus`**

- [ ] **Step 5: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt app/src/main/java/com/example/financeflow/ui/components/TransactionTypeToggle.kt
git commit -m "Redesign Home screen: glass rows, animated toggle, Phosphor icons"
```

- [ ] **Step 7: Report to user** — summarize what changed on Home before continuing to Task 12,
  per the user's request to check in after each screen.

---

## Task 12: Add/Edit Transaction screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt`

**Interfaces:**
- Consumes: `GlassTextField` (Task 7), `GlassSegmentedControl` (Task 8), `selectionRing` (Task 3),
  `GlassAlpha` (Task 1), `LocalSnackbarController` (Task 9), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace flat `OutlinedTextField`s with `GlassTextField`**

Amount field, note field, and any label field become `GlassTextField(...)` with the same
`value`/`onValueChange` wiring already present.

- [ ] **Step 2: Replace the type/income-expense segmented rows with `GlassSegmentedControl`**

- [ ] **Step 3: Fix the quick-add-category icon grid's selection state**

Replace the current `Box` + `CircleShape` + `primary.copy(alpha = 0.3f)` selection treatment with
`Modifier.selectionRing(selected = isSelected, shape = CircleShape, color = MaterialTheme.colorScheme.primary)`,
and replace the raw `0.3f` alpha (if still used for the fill wash, not the ring) with
`GlassAlpha.selectedTint`.

- [ ] **Step 4: Call the snackbar controller on save**

At the point where the screen currently navigates back / returns after a successful save, add
`LocalSnackbarController.current.show("Saved")` before navigating away.

- [ ] **Step 5: Swap `ArrowBack`/`Check` icons to `PhosphorIcons.Regular.ArrowLeft`/`Check`**

- [ ] **Step 6: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt
git commit -m "Redesign Add/Edit Transaction screen: glass fields, animated selection, save feedback"
```

- [ ] **Step 8: Report to user** — summarize what changed before continuing to Task 13.

---

## Task 13: Transactions screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt`

**Interfaces:**
- Consumes: `GlassTextField` (Task 7), `GlassRow` (Task 6), `GlassDialog` (Task 5, now Overlay
  tier), `GlassSegmentedControl` (Task 8), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace the flat search `OutlinedTextField` with `GlassTextField`**

Keep its leading `Search` icon, swapped to `PhosphorIcons.Regular.MagnifyingGlass`.

- [ ] **Step 2: Move `DateRangePickerDialog` onto `GlassDialog`**

This is the one surface the audit found entirely outside the glass system (a plain M3 `Surface` +
`RoundedCornerShape(16.dp)`). Replace that `Surface` wrapper with `GlassDialog` (which is now
Overlay-tier per Task 5), keeping the `DateRangePicker` content unchanged inside it.

- [ ] **Step 3: Replace the transaction list rows with `GlassRow`**

Same treatment as Task 11 Step 1.

- [ ] **Step 4: Replace `TransactionTypeToggle` and any other segmented usage**

Already delegates to `GlassSegmentedControl` as of Task 11 — no change needed here beyond
confirming the call site still compiles.

- [ ] **Step 5: Swap `Add`/`DateRange` icons to `PhosphorIcons.Regular.Plus`/`CalendarBlank`**

- [ ] **Step 6: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt
git commit -m "Redesign Transactions screen: glass search/rows, dialog onto glass system"
```

- [ ] **Step 8: Report to user** — summarize what changed before continuing to Task 14.

---

## Task 14: Categories screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt`

**Interfaces:**
- Consumes: `GlassRow` (Task 6), `selectionRing` (Task 3), `GlassAlpha` (Task 1),
  `LocalSnackbarController` (Task 9), `GlassDialog` (Task 5), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace the flat `CategoryRow` with `GlassRow`**

- [ ] **Step 2: Replace hardcoded alphas with `GlassAlpha` tokens**

`SwipeToDismissBox`'s `Expense.copy(alpha = 0.25f)` delete background → `Expense.copy(alpha = GlassAlpha.destructiveTint)`.
The icon swatch `Box`'s `swatch.copy(alpha = 0.22f)` → `swatch.copy(alpha = GlassAlpha.containerTint)`.

- [ ] **Step 3: Fix the icon-grid and color-swatch-grid selection states in the add/edit dialog**

Same treatment as Task 12 Step 3: replace the `0.3f`-alpha + variable-border-width selection with
`Modifier.selectionRing(...)`, alpha wash (if any remains) → `GlassAlpha.selectedTint`.

- [ ] **Step 4: Call the snackbar controller on delete**

After a successful swipe-to-delete completes, `LocalSnackbarController.current.show("Deleted")`.

- [ ] **Step 5: Swap `ArrowBack`/`Add`/`Delete` icons to Phosphor equivalents**

- [ ] **Step 6: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt
git commit -m "Redesign Categories screen: glass rows, tokenized alphas, animated selection, delete feedback"
```

- [ ] **Step 8: Report to user** — summarize what changed before continuing to Task 15.

---

## Task 15: Budgets screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt`

**Interfaces:**
- Consumes: `Warning`/`WarningContainer`/`OnWarning` (Task 1), `GlassAlpha` (Task 1),
  `GlassSegmentedControl` (Task 8), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Add the three-state progress coloring**

Wherever `BudgetCard`/`LinearProgressIndicator` currently picks `Income` vs `Expense` based on
spent/limit ratio, add a third branch: `< 0.8f` → `Income`, `in 0.8f..<1.0f` → `Warning`, `>= 1.0f`
→ `Expense`. Replace the progress track's `0.15f` hardcoded alpha with `GlassAlpha.trackTint`.

- [ ] **Step 2: Replace `SetBudgetDialog`'s segmented rows with `GlassSegmentedControl`**

- [ ] **Step 3: Swap the `Warning` icon reference to `PhosphorIcons.Regular.WarningCircle`**
  (or `PhosphorIcons.Fill.WarningCircle` for the over-budget state, for visual weight matching the
  new three-state coloring)

- [ ] **Step 4: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt
git commit -m "Redesign Budgets screen: three-state warning coloring, glass segmented dialog"
```

- [ ] **Step 6: Report to user** — summarize what changed before continuing to Task 16.

---

## Task 16: Reports screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt`

**Interfaces:**
- Consumes: `GlassSegmentedControl` (Task 8), `GlassRow` (Task 6), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace the Overview/Budgets tab row and period row with `GlassSegmentedControl`**

- [ ] **Step 2: Animate `CategoryPieChart`'s arc entrance**

Wrap the per-category sweep-angle values in `Animatable(0f)` (one per slice, or a single shared
progress `Animatable` multiplying each slice's target sweep angle), `LaunchedEffect(Unit)` that
calls `animateTo(1f, tween(600, delayMillis = index * 60))` staggered per slice index, and use the
animated value in place of the currently-static `drawArc` sweep angle.

```kotlin
val progress = remember { Animatable(0f) }
LaunchedEffect(Unit) {
    progress.animateTo(1f, animationSpec = tween(durationMillis = 600))
}
// in each drawArc call:
sweepAngle = targetSweepAngle * progress.value
```

(Apply the per-slice `delayMillis = index * 60` stagger by giving each slice its own `Animatable`
if the current `CategoryPieChart` implementation draws all slices in one `Canvas` pass — one
shared `Animatable` with no stagger is an acceptable simpler fallback if per-slice staggering
proves awkward against the existing draw loop structure.)

- [ ] **Step 3: Animate `ReportBarChart`'s entrance**

Wrap the Vico `CartesianChartHost` in `AnimatedVisibility(visibleState = remember { MutableTransitionState(false).apply { targetState = true } }, enter = fadeIn() + slideInVertically { it / 4 })`.

- [ ] **Step 4: Replace `DailyReportList`'s flat rows with `GlassRow`**

- [ ] **Step 5: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt
git commit -m "Redesign Reports screen: chart entrance animations, glass segmented tabs and rows"
```

- [ ] **Step 7: Report to user** — summarize what changed before continuing to Task 17.

---

## Task 17: Recurring Rules screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt`

**Interfaces:**
- Consumes: `GlassRow` (Task 6), `LocalSnackbarController` (Task 9), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace the flat `RecurringRuleRow` with `GlassRow`**

- [ ] **Step 2: Tint the M3 `Switch`'s colors**

Apply `SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer)` (or equivalent accent-tinted colors) instead of the current default M3 `Switch` coloring.

- [ ] **Step 3: Call the snackbar controller on pause/resume or delete, if either action exists on this screen**

`LocalSnackbarController.current.show(...)` with an appropriate message ("Paused"/"Resumed"/"Deleted") at whichever point the screen currently mutates a rule's state.

- [ ] **Step 4: Swap the `Add` icon to `PhosphorIcons.Regular.Plus`**

- [ ] **Step 5: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt
git commit -m "Redesign Recurring Rules screen: glass rows, accent-tinted switch, feedback"
```

- [ ] **Step 7: Report to user** — summarize what changed before continuing to Task 18.

---

## Task 18: Settings screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `GlassSegmentedControl` (Task 8), `PhosphorIcons` (Task 10).

- [ ] **Step 1: Replace the language/currency segmented rows with `GlassSegmentedControl`**

- [ ] **Step 2: Swap the `ChevronRight` icon to `PhosphorIcons.Regular.CaretRight`**

- [ ] **Step 3: Confirm the two existing `GlassCard` nav rows (Categories, Recurring) and
  `GlassButton`x2 (export/import) still read correctly** now that `GlassCard`/`GlassButton` source
  from tiers (Tasks 4–5) — no code change expected here, just a compile/visual check.

- [ ] **Step 4: Verify it compiles**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt
git commit -m "Redesign Settings screen: glass segmented rows, Phosphor icons"
```

- [ ] **Step 6: Report to user** — summarize what changed. This is the last screen.

---

## Task 19: Update `app/CLAUDE.md`'s Design & Styling section

**Files:**
- Modify: `app/CLAUDE.md`

**Interfaces:**
- Consumes: nothing (documentation only).

The current "Glassmorphism" paragraph in `app/CLAUDE.md` §5 says to avoid glass "on dense
transaction lists or anywhere numbers need to be read quickly." This pass deliberately supersedes
that with the tiered approach — update the doc so future work doesn't regress against stale
guidance.

- [ ] **Step 1: Replace the Glassmorphism paragraph**

```markdown
**Glassmorphism (modern frosted-glass look):**
- Achieved via a 4-tier elevation system (`GlassTier` in `ui/theme/GlassTier.kt`): `Row` (list
  rows — faint fill, no blur/shadow, tuned low specifically so financial figures stay scannable),
  `Card` (summary/budget cards, settings rows), `Interactive` (buttons, chips, FAB — brighter, to
  cue tappability), `Overlay` (dialogs — boldest). Nothing in the UI is flat except the base
  background.
- Legibility is still the hard constraint on dense lists — that's *why* `Row` is the most
  restrained tier, not a reason to avoid glass there entirely.
- Pair with a dark or muted background so the frosted cards have contrast to "float" against.
```

- [ ] **Step 2: Commit**

```bash
git add app/CLAUDE.md
git commit -m "Update CLAUDE.md glassmorphism guidance to match the elevation-tier system"
```

---

## Self-Review Notes

- **Spec coverage:** §3.1 Warning color → Task 1, 15. §3.2 Spacing → Task 1 (defined; applied
  ad-hoc during screen tasks where a `Spacer` is touched — not exhaustively enumerated per screen,
  consistent with the spec's "not a strict rename of every literal" framing). §3.3 Radius → Task
  1, 6. §3.4 Alpha → Task 1, 12, 14. §3.5 Elevation tiers → Tasks 1, 4, 5, 6. §4 shared components
  → Tasks 2–9. §5 icon migration → Task 10 (+ per-screen icon swaps in 11–18). §6 micro-
  interactions → Tasks 2, 3, 8 (foundations) + 11 (toggle), 16 (charts), 9/12/14/17 (snackbar). §7
  screen table → Tasks 11–18, one-to-one. §8 testing → Global Constraints + every task's compile
  step. §9 risks → called out inline in Task 15 (warning tier) and Task 10 (icon mapping
  fallback).
- **Placeholder scan:** no TBD/TODO; every step has literal code or an explicit, bounded
  fallback instruction (icon name mismatches, per-slice chart staggering) rather than an open-
  ended one.
- **Type consistency:** `GlassTier`/`GlassTierSpec`/`spec()` (Task 1) referenced identically in
  Tasks 4–9. `pressScale(interactionSource, pressedScale)` (Task 2) signature matches its use in
  Tasks 5–6. `selectionRing(selected, shape, color, width)` (Task 3) matches Tasks 12/14.
  `GlassSegmentedControl<T>(options, selected, onSelect, label, modifier)` (Task 8) matches Tasks
  11/13/15/16/18. `LocalSnackbarController`/`SnackbarController.show(String)` (Task 9) matches
  Tasks 12/14/17.
