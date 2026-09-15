package com.example.financeflow.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.backup.BackupDirtyTracker
import com.example.financeflow.data.backup.BackupFileInfo
import com.example.financeflow.data.backup.BackupFileStore
import com.example.financeflow.data.backup.BackupManager
import com.example.financeflow.data.backup.BackupTrigger
import com.example.financeflow.data.backup.parseBackup
import com.example.financeflow.data.backup.toJson
import com.example.financeflow.locale.BackupFolderPreferences
import com.example.financeflow.locale.BackupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val context: Context,
    private val db: AppDatabase
) : ViewModel() {
    val folder: StateFlow<Uri?> = BackupFolderPreferences.folderFlow(context)
    val lastResult: StateFlow<BackupResult?> = BackupFolderPreferences.lastResultFlow(context)

    private val _backups = MutableStateFlow<List<BackupFileInfo>>(emptyList())
    val backups: StateFlow<List<BackupFileInfo>> = _backups.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val folderUri = folder.value ?: run { _backups.value = emptyList(); return }
        viewModelScope.launch {
            _backups.value = runCatching { BackupFileStore.listBackups(context, folderUri) }.getOrDefault(emptyList())
        }
    }

    fun setFolder(uri: Uri) {
        BackupFolderPreferences.setFolder(context, uri)
        refresh()
    }

    suspend fun backupNow(): Boolean {
        val folderUri = folder.value ?: return false
        return try {
            val payload = BackupManager(db).exportPayload()
            BackupFileStore.writeBackup(context, folderUri, payload.toJson(), BackupTrigger.MANUAL)
            // A manual backup also satisfies "since the last backup" for the on-close trigger.
            BackupDirtyTracker.markClean()
            refresh()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restore(uri: Uri): Boolean = try {
        val json = BackupFileStore.readBackup(context, uri)
        BackupManager(db).importPayload(parseBackup(json))
        true
    } catch (e: Exception) {
        false
    }
}
