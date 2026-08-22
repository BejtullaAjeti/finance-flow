package com.example.financeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Frequency
import com.example.financeflow.data.RecurringRule
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.ui.components.DateField
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.RecurringRuleViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberRecurringRuleViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleScreen(
    ruleId: Long?,
    onDone: () -> Unit,
    recurringRuleViewModel: RecurringRuleViewModel = rememberRecurringRuleViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val allRules by recurringRuleViewModel.rules.collectAsState()
    val existing = remember(allRules, ruleId) {
        ruleId?.let { id -> allRules.find { it.id == id } }
    }
    val filteredCategories by categoryViewModel.filteredCategories.collectAsState()

    val displayCurrency by CurrencyPreferences.flow(LocalContext.current).collectAsState()

    var initialized by remember { mutableStateOf(ruleId == null) }
    var label by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.PERSONAL) }
    var isIncome by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(Frequency.MONTHLY) }
    var customIntervalText by remember { mutableStateOf("") }
    var nextDueDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var currency by remember { mutableStateOf(displayCurrency) }

    LaunchedEffect(existing) {
        if (existing != null && !initialized) {
            label = existing.label
            amountText = existing.amount.toString()
            type = existing.type
            isIncome = existing.isIncome
            frequency = existing.frequency
            customIntervalText = existing.customIntervalDays?.toString().orEmpty()
            nextDueDate = existing.nextDueDate
            currency = existing.currency
            initialized = true
        }
    }

    LaunchedEffect(type) {
        categoryViewModel.setTypeFilter(categoryTypeFor(type))
    }

    LaunchedEffect(existing, filteredCategories) {
        if (existing != null && selectedCategory == null) {
            selectedCategory = filteredCategories.find { it.id == existing.categoryId }
        }
    }

    val canSave = label.isNotBlank() &&
        amountText.toDoubleOrNull()?.let { it > 0 } == true &&
        selectedCategory != null &&
        (frequency != Frequency.CUSTOM || (customIntervalText.toIntOrNull()?.let { it > 0 } == true))

    fun save() {
        val amount = amountText.toDoubleOrNull() ?: return
        val category = selectedCategory ?: return
        val rule = RecurringRule(
            id = existing?.id ?: 0,
            label = label.trim(),
            amount = amount,
            currency = currency,
            categoryId = category.id,
            type = type,
            isIncome = isIncome,
            frequency = frequency,
            customIntervalDays = if (frequency == Frequency.CUSTOM) customIntervalText.toIntOrNull() else null,
            nextDueDate = nextDueDate,
            active = existing?.active ?: true
        )
        if (existing != null) {
            recurringRuleViewModel.updateRule(rule)
        } else {
            recurringRuleViewModel.addRule(rule)
        }
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing != null) R.string.recurring_edit_title else R.string.recurring_add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = ::save, enabled = canSave) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.recurring_label_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text(stringResource(R.string.field_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    icon = {},
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.toggle_expense)) }
                SegmentedButton(
                    icon = {},
                    selected = isIncome,
                    onClick = { isIncome = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.toggle_income)) }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    icon = {},
                    selected = type == TransactionType.PERSONAL,
                    onClick = { type = TransactionType.PERSONAL; selectedCategory = null },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.type_personal)) }
                SegmentedButton(
                    icon = {},
                    selected = type == TransactionType.BUSINESS,
                    onClick = { type = TransactionType.BUSINESS; selectedCategory = null },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.type_business)) }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Currency.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        icon = {},
                        selected = currency == option,
                        onClick = { currency = option },
                        shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
                    ) { Text(option.name) }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(text = stringResource(R.string.field_category_label), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (filteredCategories.isEmpty()) {
                Text(text = stringResource(R.string.categories_picker_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory?.id == category.id,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(text = stringResource(R.string.recurring_frequency_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Frequency.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        icon = {},
                        selected = frequency == option,
                        onClick = { frequency = option },
                        shape = SegmentedButtonDefaults.itemShape(index, Frequency.entries.size)
                    ) {
                        Text(
                            when (option) {
                                Frequency.WEEKLY -> stringResource(R.string.label_weekly)
                                Frequency.MONTHLY -> stringResource(R.string.label_monthly)
                                Frequency.CUSTOM -> stringResource(R.string.label_custom)
                            }
                        )
                    }
                }
            }

            if (frequency == Frequency.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customIntervalText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*$"))) {
                            customIntervalText = input
                        }
                    },
                    label = { Text(stringResource(R.string.recurring_every_n_days_field_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            DateField(label = stringResource(R.string.recurring_next_due_date_label), date = nextDueDate, onDateChange = { nextDueDate = it })
        }
    }
}
