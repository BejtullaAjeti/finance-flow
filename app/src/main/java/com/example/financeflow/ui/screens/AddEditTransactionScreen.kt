package com.example.financeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Briefcase
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.User
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Currency
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.HintPreferences
import com.example.financeflow.locale.LastUsedTypePreferences
import com.example.financeflow.ui.components.DateField
import com.example.financeflow.ui.components.GlassFilterChip
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.components.GlassTextField
import com.example.financeflow.ui.components.InlineHint
import com.example.financeflow.ui.components.LocalSnackbarController
import com.example.financeflow.ui.components.QuickAddCategoryDialog
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.TransactionViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberTransactionViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    suggestedType: TransactionType? = null,
    onDone: () -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val existing = remember(allTransactions, transactionId) {
        transactionId?.let { id -> allTransactions.find { it.id == id } }
    }
    val filteredCategories by categoryViewModel.filteredCategories.collectAsState()

    val context = LocalContext.current
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    var showCategoryHint by remember { mutableStateOf(!HintPreferences.isCategoryPickerHintDismissed(context)) }
    val snackbarController = LocalSnackbarController.current
    val savedMessage = stringResource(R.string.action_save)

    var initialized by remember { mutableStateOf(transactionId == null) }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(suggestedType ?: LastUsedTypePreferences.get(context)) }
    var isIncome by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var currency by remember { mutableStateOf(displayCurrency) }
    var showQuickAddCategory by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !initialized) {
            amountText = existing.amount.toString()
            type = existing.type
            isIncome = existing.isIncome
            date = existing.date
            note = existing.note.orEmpty()
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

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (transactionId == null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val canSave = amountText.toDoubleOrNull()?.let { it > 0 } == true && selectedCategory != null

    fun save() {
        val amount = amountText.toDoubleOrNull() ?: return
        val category = selectedCategory ?: return
        val transaction = Transaction(
            id = existing?.id ?: 0,
            amount = amount,
            currency = currency,
            type = type,
            categoryId = category.id,
            date = date,
            note = note.ifBlank { null },
            isIncome = isIncome,
            recurringId = existing?.recurringId
        )
        if (existing != null) {
            transactionViewModel.updateTransaction(transaction)
        } else {
            transactionViewModel.addTransaction(transaction)
        }
        LastUsedTypePreferences.set(context, type)
        snackbarController.show(savedMessage)
        onDone()
    }

    val expenseLabel = stringResource(R.string.toggle_expense)
    val incomeLabel = stringResource(R.string.toggle_income)
    val personalLabel = stringResource(R.string.type_personal)
    val businessLabel = stringResource(R.string.type_business)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing != null) R.string.transaction_edit_title else R.string.transaction_add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = ::save, enabled = canSave) {
                        Icon(PhosphorIcons.Regular.Check, contentDescription = stringResource(R.string.action_save))
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
            GlassTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = stringResource(R.string.field_amount_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(16.dp))

            GlassSegmentedControl(
                options = listOf(false, true),
                selected = isIncome,
                onSelect = { isIncome = it },
                label = { if (it) incomeLabel else expenseLabel }
            )

            Spacer(Modifier.height(12.dp))

            GlassSegmentedControl(
                options = listOf(TransactionType.PERSONAL, TransactionType.BUSINESS),
                selected = type,
                onSelect = { type = it; selectedCategory = null },
                label = { if (it == TransactionType.PERSONAL) personalLabel else businessLabel },
                icon = { if (it == TransactionType.PERSONAL) PhosphorIcons.Regular.User else PhosphorIcons.Regular.Briefcase }
            )

            Spacer(Modifier.height(12.dp))

            GlassSegmentedControl(
                options = Currency.entries,
                selected = currency,
                onSelect = { currency = it },
                label = { it.name }
            )

            Spacer(Modifier.height(20.dp))

            Text(text = stringResource(R.string.field_category_label), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCategories.forEach { category ->
                    GlassFilterChip(
                        selected = selectedCategory?.id == category.id,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) }
                    )
                }
                GlassFilterChip(
                    selected = false,
                    onClick = { showQuickAddCategory = true },
                    label = { Text(stringResource(R.string.categories_add_new_chip)) }
                )
            }

            if (showCategoryHint) {
                InlineHint(
                    text = stringResource(R.string.hint_category_picker),
                    onDismiss = {
                        HintPreferences.dismissCategoryPickerHint(context)
                        showCategoryHint = false
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            DateField(label = stringResource(R.string.field_date_label), date = date, onDateChange = { date = it })

            Spacer(Modifier.height(12.dp))

            GlassTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.field_note_optional_label),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showQuickAddCategory) {
        QuickAddCategoryDialog(
            defaultType = categoryTypeFor(type),
            onDismiss = { showQuickAddCategory = false },
            onSave = { category ->
                categoryViewModel.addCategory(category) { id ->
                    selectedCategory = category.copy(id = id)
                }
                showQuickAddCategory = false
            }
        )
    }
}
