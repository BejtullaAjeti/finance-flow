package com.example.financeflow.ui.theme

import androidx.compose.ui.unit.dp

// The one corner-radius value for every rounded-rectangle surface in the app — cards, list rows,
// buttons, dialogs, inputs, the FAB, and chips/selectors alike, so an outer container and
// whatever's nested inside it (a button, a chip, a text field) always share the exact same
// corner. 12dp reads as a clearly rounded, squared-off rect rather than a stadium even on the
// shortest controls (FilterChip's fixed 32dp M3 minimum height — 12dp is well under the 16dp
// that would be a full pill there). True circles/pills (ConfirmButton, icon swatches,
// GlassSegmentedControl's opt-in pill mode) are a different shape category and don't use this.
object Radius {
    val medium = 12.dp
}
