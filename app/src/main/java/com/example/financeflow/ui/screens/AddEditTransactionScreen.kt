package com.example.financeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.ui.components.DateField
import com.example.financeflow.viewmodel.CategoryViewModel
import com.example.financeflow.viewmodel.TransactionViewModel
import com.example.financeflow.viewmodel.rememberCategoryViewModel
import com.example.financeflow.viewmodel.rememberTransactionViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    onDone: () -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val existing = remember(allTransactions, transactionId) {
        transactionId?.let { id -> allTransactions.find { it.id == id } }
    }
    val filteredCategories by categoryViewModel.filteredCategories.collectAsState()

    var initialized by remember { mutableStateOf(transactionId == null) }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.PERSONAL) }
    var isIncome by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(existing) {
        if (existing != null && !initialized) {
            amountText = existing.amount.toString()
            type = existing.type
            isIncome = existing.isIncome
            date = existing.date
            note = existing.note.orEmpty()
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
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing != null) R.string.transaction_edit_title else R.string.transaction_add_title)) },
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
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text(stringResource(R.string.field_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.toggle_expense)) }
                SegmentedButton(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.toggle_income)) }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.PERSONAL,
                    onClick = { type = TransactionType.PERSONAL; selectedCategory = null },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.type_personal)) }
                SegmentedButton(
                    selected = type == TransactionType.BUSINESS,
                    onClick = { type = TransactionType.BUSINESS; selectedCategory = null },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.type_business)) }
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

            DateField(label = stringResource(R.string.field_date_label), date = date, onDateChange = { date = it })

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.field_note_optional_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
