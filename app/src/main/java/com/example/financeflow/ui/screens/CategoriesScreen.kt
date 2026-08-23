package com.example.financeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Briefcase
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.User
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.ui.components.CategoryColorPalette
import com.example.financeflow.ui.components.CategoryIcons
import com.example.financeflow.ui.components.ConfirmButton
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassDialog
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.GlassRow
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.components.GlassTextField
import com.example.financeflow.ui.components.categoryTypeLabel
import com.example.financeflow.ui.components.periodLabel
import com.example.financeflow.ui.components.selectionRing
import com.example.financeflow.ui.components.toCategoryColor
import com.example.financeflow.ui.theme.GlassTier
import com.example.financeflow.viewmodel.CategoryDeleteBlockReason
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val inUseMessage = stringResource(R.string.categories_delete_blocked)
    val lastOfTypeMessage = stringResource(R.string.categories_delete_blocked_last)
    val deletedMessage = stringResource(R.string.categories_delete_success)
    val undoLabel = stringResource(R.string.action_undo)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                GlassCard(tier = GlassTier.Overlay, contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(data.visuals.message)
                        data.visuals.actionLabel?.let { actionLabel ->
                            TextButton(onClick = { data.performAction() }) { Text(actionLabel) }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            GlassFab(
                onClick = { editingCategory = null; showDialog = true },
                contentDescription = stringResource(R.string.categories_add_content_description)
            ) {
                Icon(PhosphorIcons.Regular.Plus, contentDescription = null)
            }
        }
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.categories_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    SwipeToDeleteCategoryRow(
                        category = category,
                        onClick = { editingCategory = category; showDialog = true },
                        onDelete = {
                            categoryViewModel.deleteCategory(
                                category,
                                onDeleted = {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedMessage.format(category.name),
                                            actionLabel = undoLabel
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            categoryViewModel.addCategory(category.copy(id = 0))
                                        }
                                    }
                                },
                                onBlocked = { reason ->
                                    val message = when (reason) {
                                        CategoryDeleteBlockReason.IN_USE -> inUseMessage
                                        CategoryDeleteBlockReason.LAST_OF_TYPE -> lastOfTypeMessage
                                    }
                                    scope.launch { snackbarHostState.showSnackbar(message.format(category.name)) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AddEditCategoryDialog(
            editing = editingCategory,
            onDismiss = { showDialog = false },
            onSave = { category ->
                if (editingCategory != null) {
                    categoryViewModel.updateCategory(category)
                } else {
                    categoryViewModel.addCategory(category)
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteCategoryRow(
    category: Category,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Never let the box auto-remove itself — the row's actual removal (or spring-back, if the
    // delete was blocked) is driven by `categories` re-emitting once the real DB delete resolves.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(PhosphorIcons.Regular.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    ) {
        CategoryRow(category = category, onClick = onClick)
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit
) {
    val currencyFormat = rememberCurrencyFormat(category.budgetLimitCurrency)
    val swatch = category.color.toCategoryColor()
    GlassRow(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(swatch, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    CategoryIcons.resolve(category.icon),
                    contentDescription = null,
                    tint = if (swatch.luminance() > 0.5f) Color.Black else Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = buildString {
                        append(categoryTypeLabel(category.type))
                        category.budgetLimit?.let {
                            append(" · ")
                            append(
                                stringResource(
                                    R.string.categories_budget_suffix,
                                    currencyFormat.format(it),
                                    periodLabel(category.budgetPeriod)
                                )
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCategoryDialog(
    editing: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: CategoryType.PERSONAL) }
    var icon by remember { mutableStateOf(editing?.icon) }
    var color by remember { mutableStateOf(editing?.color ?: CategoryColorPalette.first()) }
    var budgetText by remember { mutableStateOf(editing?.budgetLimit?.toString().orEmpty()) }
    var budgetCurrency by remember { mutableStateOf(editing?.budgetLimitCurrency ?: Currency.EUR) }
    val categoryTypeLabels = CategoryType.entries.associateWith { categoryTypeLabel(it) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editing != null) R.string.categories_edit_title else R.string.categories_add_title
                )
            )
        },
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
                    label = { categoryTypeLabels[it] ?: "" },
                    icon = {
                        when (it) {
                            CategoryType.PERSONAL -> PhosphorIcons.Regular.User
                            CategoryType.BUSINESS -> PhosphorIcons.Regular.Briefcase
                            CategoryType.BOTH -> null
                        }
                    }
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
                                .selectionRing(selected = selected, shape = CircleShape, color = color.toCategoryColor())
                                .clip(CircleShape)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(vector, contentDescription = key)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.categories_color_label), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColorPalette.forEach { hex ->
                        val swatchColor = hex.toCategoryColor()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .selectionRing(
                                    selected = color == hex,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    width = 3.dp
                                )
                                .clip(CircleShape)
                                .background(swatchColor, CircleShape)
                                .clickable { color = hex }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                GlassTextField(
                    value = budgetText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            budgetText = input
                        }
                    },
                    label = stringResource(R.string.categories_budget_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                GlassSegmentedControl(
                    options = Currency.entries,
                    selected = budgetCurrency,
                    onSelect = { budgetCurrency = it },
                    label = { it.name }
                )
            }
        },
        confirmButton = {
            ConfirmButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Category(
                            id = editing?.id ?: 0,
                            name = name.trim(),
                            type = type,
                            budgetLimit = budgetText.toDoubleOrNull(),
                            budgetLimitCurrency = budgetCurrency,
                            // This dialog has no period selector of its own (that lives in
                            // Budgets' SetBudgetDialog) - preserve whatever period the category
                            // already had rather than silently resetting it to Monthly.
                            budgetPeriod = editing?.budgetPeriod ?: ReportPeriod.MONTH,
                            icon = icon,
                            color = color
                        )
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
