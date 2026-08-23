package com.example.financeflow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.data.TransactionType

@Composable
fun TransactionTypeToggle(
    selected: TransactionType?,
    onSelect: (TransactionType?) -> Unit,
    modifier: Modifier = Modifier
) {
    // ponytail: labels resolved here (not cached) so they re-resolve on every recomposition
    // instead of being frozen in whatever locale was active at first composition.
    val personalLabel = stringResource(R.string.type_personal)
    val businessLabel = stringResource(R.string.type_business)
    val combinedLabel = stringResource(R.string.type_combined)

    GlassSegmentedControl(
        options = listOf(TransactionType.PERSONAL, TransactionType.BUSINESS, null),
        selected = selected,
        onSelect = onSelect,
        label = { type ->
            when (type) {
                TransactionType.PERSONAL -> personalLabel
                TransactionType.BUSINESS -> businessLabel
                null -> combinedLabel
            }
        },
        modifier = modifier
    )
}
