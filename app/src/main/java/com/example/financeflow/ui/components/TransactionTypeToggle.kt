package com.example.financeflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.data.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeToggle(
    selected: TransactionType?,
    onSelect: (TransactionType?) -> Unit,
    modifier: Modifier = Modifier
) {
    // ponytail: built inside the composable (not a top-level val) so the labels re-resolve on
    // every recomposition instead of being frozen in whatever locale was active at class-load.
    val options = listOf(
        TransactionType.PERSONAL to stringResource(R.string.type_personal),
        TransactionType.BUSINESS to stringResource(R.string.type_business),
        null to stringResource(R.string.type_combined)
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                icon = {},
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}
