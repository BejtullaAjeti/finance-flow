package com.example.financeflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Category
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.rememberCurrencyFormat
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassFab
import com.example.financeflow.ui.components.TransactionRow
import com.example.financeflow.ui.components.TransactionTypeToggle
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.TransactionViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberTransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onEditTransaction: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val context = LocalContext.current
    val filter by transactionViewModel.currentListFilter.collectAsState()
    val transactions by transactionViewModel.filteredList.collectAsState()
    val categories by categoryViewModel.filteredCategories.collectAsState()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
    val currencyFormat = rememberCurrencyFormat(displayCurrency)
    val dateFormat = rememberDateFormat("MMM d")
    val uncategorized = stringResource(R.string.category_uncategorized)

    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = onAddTransaction, contentDescription = stringResource(R.string.home_add_transaction_content_description)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = { query ->
                transactionViewModel.updateListFilter { it.copy(searchQuery = query) }
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.search_notes_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        TransactionTypeToggle(
            selected = filter.type,
            onSelect = { type ->
                transactionViewModel.updateListFilter { it.copy(type = type, categoryId = null) }
                categoryViewModel.setTypeFilter(type?.let(::categoryTypeFor))
            }
        )

        Spacer(Modifier.height(12.dp))

        CategoryDropdown(
            categories = categories,
            selectedCategoryId = filter.categoryId,
            onSelect = { categoryId ->
                transactionViewModel.updateListFilter { it.copy(categoryId = categoryId) }
            }
        )

        Spacer(Modifier.height(12.dp))

        DateRangeField(
            start = filter.startDate,
            end = filter.endDate,
            onClick = { showDateRangePicker = true }
        )

        Spacer(Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Text(
                text = stringResource(R.string.transactions_empty),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        categoryName = categoryNames[transaction.categoryId] ?: uncategorized,
                        displayAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates),
                        currencyFormat = currencyFormat,
                        dateFormat = dateFormat,
                        displayCurrency = displayCurrency,
                        onClick = { onEditTransaction(transaction.id) }
                    )
                }
            }
        }
    }

    if (showDateRangePicker) {
        DateRangePickerDialog(
            initialStart = filter.startDate,
            initialEnd = filter.endDate,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { start, end ->
                transactionViewModel.updateListFilter { it.copy(startDate = start, endDate = end) }
                showDateRangePicker = false
            }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allCategoriesLabel = stringResource(R.string.category_filter_all)
    val selectedName = categories.find { it.id == selectedCategoryId }?.name ?: allCategoriesLabel

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allCategoriesLabel) },
                onClick = { onSelect(null); expanded = false }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DateRangeField(start: LocalDate?, end: LocalDate?, onClick: () -> Unit) {
    val formatter = rememberDateFormat("MMM d, yyyy")
    val label = when {
        start == null && end == null -> stringResource(R.string.date_range_all)
        start != null && end != null -> stringResource(R.string.date_range_between, start.format(formatter), end.format(formatter))
        start != null -> stringResource(R.string.date_range_from, start.format(formatter))
        else -> stringResource(R.string.date_range_until, end!!.format(formatter))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = stringResource(R.string.date_range_label), style = MaterialTheme.typography.labelSmall)
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Rounded.DateRange, contentDescription = stringResource(R.string.date_range_change_content_description))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalDate?) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialSelectedEndDateMillis = initialEnd?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)
        ) {
            Column {
                DateRangePicker(state = state, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onConfirm(null, null) }) { Text(stringResource(R.string.action_clear)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = {
                        val start = state.selectedStartDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        val end = state.selectedEndDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        onConfirm(start, end)
                    }) { Text(stringResource(R.string.action_apply)) }
                }
            }
        }
    }
}
