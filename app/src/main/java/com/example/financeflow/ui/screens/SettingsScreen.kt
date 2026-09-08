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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.financeflow.ui.components.ConfirmButton
import com.example.financeflow.ui.components.GlassButton
import com.example.financeflow.ui.components.GlassRow
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.components.GlassDialog
import com.example.financeflow.ui.components.GlassSegmentedControl
import com.example.financeflow.ui.theme.ThemeMode
import com.example.financeflow.ui.theme.ThemePreferences
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
    val themeMode by ThemePreferences.flow(context).collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Longest form in the app (five sections); it had no scroll at all, so the currency
            // section and the attribution link were unreachable on a normal phone.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
    ) {
        // headlineSmall to match Home / Transactions / Reports (was headlineMedium).
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.lg)
        )

        GlassRow(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToCategories) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_categories), style = MaterialTheme.typography.bodyLarge)
                Icon(PhosphorIcons.Regular.CaretRight, contentDescription = null)
            }
        }

        Spacer(Modifier.height(Spacing.md))

        GlassRow(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToRecurring) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_recurring), style = MaterialTheme.typography.bodyLarge)
                Icon(PhosphorIcons.Regular.CaretRight, contentDescription = null)
            }
        }

        Spacer(Modifier.height(Spacing.xxl))

        Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))

        val albanianLabel = stringResource(R.string.language_albanian)
        val englishLabel = stringResource(R.string.language_english)
        GlassSegmentedControl(
            options = listOf("sq", "en"),
            selected = languageCode,
            onSelect = { code ->
                LocalePreferences.set(context, code)
                languageCode = code
                (context as? Activity)?.recreate()
            },
            label = { if (it == "sq") albanianLabel else englishLabel }
        )

        Spacer(Modifier.height(Spacing.xxl))

        Text(text = stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))

        val systemThemeLabel = stringResource(R.string.theme_system)
        val lightThemeLabel = stringResource(R.string.theme_light)
        val darkThemeLabel = stringResource(R.string.theme_dark)
        GlassSegmentedControl(
            options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
            selected = themeMode,
            onSelect = { ThemePreferences.set(context, it) },
            label = {
                when (it) {
                    ThemeMode.SYSTEM -> systemThemeLabel
                    ThemeMode.LIGHT -> lightThemeLabel
                    ThemeMode.DARK -> darkThemeLabel
                }
            }
        )

        Spacer(Modifier.height(Spacing.xxl))

        Text(text = stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            GlassButton(onClick = { exportLauncher.launch("financeflow-backup-${LocalDate.now()}.json") }) {
                Text(stringResource(R.string.backup_export))
            }
            GlassButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text(stringResource(R.string.backup_import))
            }
        }
        statusMessage?.let { message ->
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.xxl))

        Text(text = stringResource(R.string.settings_currency_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))

        GlassSegmentedControl(
            options = Currency.entries,
            selected = displayCurrency,
            onSelect = { CurrencyPreferences.set(context, it) },
            label = { it.name }
        )

        Spacer(Modifier.height(Spacing.sm))

        val lastUpdatedText = rates.lastUpdatedEpochMillis?.let { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            stringResource(R.string.settings_currency_last_updated, date.format(currencyDateFormat))
        } ?: stringResource(R.string.settings_currency_never_updated)

        Text(text = lastUpdatedText, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(Spacing.xs))

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

        Spacer(Modifier.height(Spacing.xxl))
    }

    pendingImportUri?.let { uri ->
        GlassDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_body)) },
            confirmButton = {
                ConfirmButton(enabled = true, onClick = {
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
                })
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
