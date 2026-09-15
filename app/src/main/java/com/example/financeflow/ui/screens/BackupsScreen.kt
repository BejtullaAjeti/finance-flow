package com.example.financeflow.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Folder
import com.adamglin.phosphoricons.regular.FloppyDisk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.financeflow.R
import com.example.financeflow.data.backup.BackupFileInfo
import com.example.financeflow.data.backup.BackupTrigger
import com.example.financeflow.locale.rememberDateFormat
import com.example.financeflow.ui.components.CancelButton
import com.example.financeflow.ui.components.ConfirmButton
import com.example.financeflow.ui.components.GlassButton
import com.example.financeflow.ui.components.GlassDialog
import com.example.financeflow.ui.components.ListRow
import com.example.financeflow.ui.components.LocalSnackbarController
import com.example.financeflow.ui.theme.Spacing
import com.example.financeflow.ui.theme.extendedColors
import com.example.financeflow.viewmodel.BackupViewModel
import com.example.financeflow.viewmodel.rememberBackupViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupsScreen(
    onBack: () -> Unit,
    backupViewModel: BackupViewModel = rememberBackupViewModel()
) {
    val context = LocalContext.current
    val folder by backupViewModel.folder.collectAsState()
    val backups by backupViewModel.backups.collectAsState()
    val lastResult by backupViewModel.lastResult.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarController = LocalSnackbarController.current
    val dateTimeFormat = rememberDateFormat("MMM d, yyyy HH:mm")

    var pendingRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }

    val backupNowSuccessMessage = stringResource(R.string.backups_backup_now_success)
    val backupNowFailedMessage = stringResource(R.string.backups_backup_now_failed)
    val restoreSuccessMessage = stringResource(R.string.backups_restore_success)
    val restoreFailedMessage = stringResource(R.string.backups_restore_failed)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            backupViewModel.setFolder(uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val defaultFolderLabel = stringResource(R.string.backups_folder_default)
    val folderDisplayName = remember(folder, defaultFolderLabel) {
        folder?.let { uri ->
            if (uri.scheme == "file") defaultFolderLabel else DocumentFile.fromTreeUri(context, uri)?.name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backups_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
        ) {
            Spacer(Modifier.height(Spacing.md))

            Text(text = stringResource(R.string.backups_folder_section), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PhosphorIcons.Regular.Folder, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = folderDisplayName ?: stringResource(R.string.backups_folder_not_set),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                GlassButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Text(
                        stringResource(
                            if (folder == null) R.string.backups_choose_folder else R.string.backups_change_folder
                        )
                    )
                }
            }

            if (lastResult != null && lastResult?.success == false) {
                Spacer(Modifier.height(Spacing.sm))
                val failedAt = remember(lastResult) {
                    Instant.ofEpochMilli(lastResult!!.timestampMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)
                }
                Text(
                    text = stringResource(R.string.backups_last_failure_banner, failedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.warning
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            GlassButton(
                onClick = {
                    isBackingUp = true
                    scope.launch {
                        val success = backupViewModel.backupNow()
                        isBackingUp = false
                        snackbarController.show(if (success) backupNowSuccessMessage else backupNowFailedMessage)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backups_backup_now))
            }

            Spacer(Modifier.height(Spacing.xxl))

            if (backups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.backups_empty), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(backups, key = { it.uri.toString() }) { backup ->
                        BackupRow(
                            backup = backup,
                            dateTimeFormat = dateTimeFormat,
                            onClick = { pendingRestore = backup }
                        )
                    }
                    item(key = "bottomSpacer") { Spacer(Modifier.height(Spacing.xl)) }
                }
            }
        }
    }

    pendingRestore?.let { backup ->
        GlassDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.backups_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backups_restore_confirm_body)) },
            confirmButton = {
                ConfirmButton(enabled = true, onClick = {
                    pendingRestore = null
                    scope.launch {
                        val success = backupViewModel.restore(backup.uri)
                        snackbarController.show(if (success) restoreSuccessMessage else restoreFailedMessage)
                    }
                })
            },
            dismissButton = {
                CancelButton(onClick = { pendingRestore = null })
            }
        )
    }
}

@Composable
private fun BackupRow(
    backup: BackupFileInfo,
    dateTimeFormat: java.time.format.DateTimeFormatter,
    onClick: () -> Unit
) {
    val formatted = remember(backup.timestampMillis) {
        Instant.ofEpochMilli(backup.timestampMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)
    }
    ListRow(
        icon = PhosphorIcons.Regular.FloppyDisk,
        swatchColor = MaterialTheme.colorScheme.primary,
        title = formatted,
        onClick = onClick,
        subtitle = {
            Text(
                text = stringResource(
                    when (backup.trigger) {
                        BackupTrigger.DAILY -> R.string.backups_row_daily
                        BackupTrigger.ON_CLOSE -> R.string.backups_row_on_close
                        BackupTrigger.MANUAL -> R.string.backups_row_manual
                    }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
