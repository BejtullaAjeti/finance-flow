package com.example.financeflow.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.backup.BackupManager
import com.example.financeflow.data.backup.parseBackup
import com.example.financeflow.data.backup.toJson
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.locale.CurrencyPreferences
import com.example.financeflow.locale.LocalePreferences
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.GlassButton
import com.example.financeflow.ui.components.GlassCard
import com.example.financeflow.ui.components.GlassDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToCategories: () -> Unit, onNavigateToRecurring: () -> Unit) {
    val context = LocalContext.current
    var languageCode by remember {
        mutableStateOf(LocalePreferences.get(context) ?: Locale.getDefault().language)
    }

    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    var isRefreshingRates by remember { mutableStateOf(false) }
    val currencyDateFormat = rememberDateFormat("MMM d, yyyy")
    val uriHandler = LocalUriHandler.current

    val exportSuccessMessage = stringResource(R.string.backup_export_success)
    val exportFailedMessage = stringResource(R.string.backup_export_failed)
    val importSuccessMessage = stringResource(R.string.backup_import_success)
    val importFailedMessage = stringResource(R.string.backup_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = try {
                val payload = BackupManager(AppDatabase.getInstance(context)).exportPayload()
                context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toJson().toByteArray()) }
                exportSuccessMessage
            } catch (e: Exception) {
                exportFailedMessage
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToCategories),
            contentPadding = 16.dp
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_categories), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToRecurring),
            contentPadding = 16.dp
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_recurring), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                icon = {},
                selected = languageCode == "sq",
                onClick = {
                    LocalePreferences.set(context, "sq")
                    languageCode = "sq"
                    (context as? Activity)?.recreate()
                },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text(stringResource(R.string.language_albanian)) }
            SegmentedButton(
                icon = {},
                selected = languageCode == "en",
                onClick = {
                    LocalePreferences.set(context, "en")
                    languageCode = "en"
                    (context as? Activity)?.recreate()
                },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text(stringResource(R.string.language_english)) }
        }

        Spacer(Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassButton(onClick = { exportLauncher.launch("financeflow-backup-${LocalDate.now()}.json") }) {
                Text(stringResource(R.string.backup_export))
            }
            GlassButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text(stringResource(R.string.backup_import))
            }
        }
        statusMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_currency_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Currency.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    icon = {},
                    selected = displayCurrency == option,
                    onClick = { CurrencyPreferences.set(context, option) },
                    shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
                ) { Text(option.name) }
            }
        }

        Spacer(Modifier.height(8.dp))

        val lastUpdatedText = rates.lastUpdatedEpochMillis?.let { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            stringResource(R.string.settings_currency_last_updated, date.format(currencyDateFormat))
        } ?: stringResource(R.string.settings_currency_never_updated)

        Text(text = lastUpdatedText, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(4.dp))

        TextButton(
            enabled = !isRefreshingRates,
            onClick = {
                isRefreshingRates = true
                scope.launch {
                    exchangeRateRepository.forceRefresh()
                    isRefreshingRates = false
                }
            }
        ) { Text(stringResource(R.string.settings_currency_refresh)) }

        Text(
            text = stringResource(R.string.settings_currency_attribution),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.clickable { uriHandler.openUri("https://www.exchangerate-api.com") }
        )
    }

    pendingImportUri?.let { uri ->
        GlassDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    scope.launch {
                        statusMessage = try {
                            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                                ?: error("Could not open file")
                            BackupManager(AppDatabase.getInstance(context)).importPayload(parseBackup(json))
                            importSuccessMessage
                        } catch (e: Exception) {
                            importFailedMessage
                        }
                    }
                }) { Text(stringResource(R.string.backup_import_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
