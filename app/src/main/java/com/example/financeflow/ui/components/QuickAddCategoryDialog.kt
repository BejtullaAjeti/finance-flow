package com.example.financeflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType

// Lightweight: name + type + icon only, unlike the full category management dialog in
// CategoriesScreen which also handles color and budget — those stay reachable from Settings.
// Shared by Add/Edit Transaction and Add Budget's "create a category on the spot" flow.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddCategoryDialog(
    defaultType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var icon by remember { mutableStateOf<String?>(null) }
    val categoryTypeLabels = CategoryType.entries.associateWith { categoryTypeLabel(it) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_add_title)) },
        text = {
            Column {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.categories_name_label),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                GlassSegmentedControl(
                    options = CategoryType.entries,
                    selected = type,
                    onSelect = { type = it },
                    label = { categoryTypeLabels[it] ?: "" }
                )

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.categories_icon_label), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CategoryIcons.Catalog.forEach { (key, vector) ->
                        val selected = icon == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .selectionRing(
                                    selected = selected,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                .clip(CircleShape)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(vector, contentDescription = key)
                        }
                    }
                }
            }
        },
        confirmButton = {
            ConfirmButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(Category(name = name.trim(), type = type, icon = icon)) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
