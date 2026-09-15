package com.example.financeflow.locale

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

private const val PREFS_NAME = "backup_settings"
private const val KEY_FOLDER_URI = "backup_folder_uri"
private const val KEY_LAST_RESULT_SUCCESS = "backup_last_result_success"
private const val KEY_LAST_RESULT_TIME = "backup_last_result_time"
private const val KEY_LAST_RESULT_MESSAGE = "backup_last_result_message"

data class BackupResult(val timestampMillis: Long, val success: Boolean, val message: String?)

/**
 * Where automatic backups are written — a persisted SAF tree URI (granted via
 * OpenDocumentTree + takePersistableUriPermission, so it survives app restarts) — plus the
 * outcome of the last automatic attempt. The Backups screen reads [lastResultFlow] to show a
 * failure banner even if the failure notification was missed or never granted, so a scheduled
 * backup failing never fails silently.
 */
object BackupFolderPreferences {
    private var folderState: MutableStateFlow<Uri?>? = null
    private var lastResultState: MutableStateFlow<BackupResult?>? = null

    fun folderFlow(context: Context): StateFlow<Uri?> =
        folderState ?: MutableStateFlow(readFolder(context)).also { folderState = it }

    fun getFolder(context: Context): Uri? = folderState?.value ?: readFolder(context)

    fun setFolder(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (uri != null) putString(KEY_FOLDER_URI, uri.toString()) else remove(KEY_FOLDER_URI)
        }
        val flow = folderState ?: MutableStateFlow(uri).also { folderState = it }
        flow.value = uri
    }

    /** Backups need to work with no setup step, so if nothing's configured yet this picks an
     * app-specific folder under external storage (no permission needed, any API level) and
     * persists it exactly like a folder chosen via SAF — the user can still switch to a SAF
     * folder later from the Backups screen. */
    fun ensureDefaultFolder(context: Context): Uri {
        getFolder(context)?.let { return it }
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val backupDir = File(baseDir, "Backups").apply { mkdirs() }
        val uri = Uri.fromFile(backupDir)
        setFolder(context, uri)
        return uri
    }

    private fun readFolder(context: Context): Uri? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_URI, null)
            ?.let(Uri::parse)

    fun lastResultFlow(context: Context): StateFlow<BackupResult?> =
        lastResultState ?: MutableStateFlow(readLastResult(context)).also { lastResultState = it }

    fun recordResult(context: Context, result: BackupResult) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_LAST_RESULT_SUCCESS, result.success)
            putLong(KEY_LAST_RESULT_TIME, result.timestampMillis)
            if (result.message != null) putString(KEY_LAST_RESULT_MESSAGE, result.message) else remove(KEY_LAST_RESULT_MESSAGE)
        }
        val flow = lastResultState ?: MutableStateFlow<BackupResult?>(result).also { lastResultState = it }
        flow.value = result
    }

    private fun readLastResult(context: Context): BackupResult? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_LAST_RESULT_TIME, -1L)
        if (time < 0) return null
        return BackupResult(
            timestampMillis = time,
            success = prefs.getBoolean(KEY_LAST_RESULT_SUCCESS, true),
            message = prefs.getString(KEY_LAST_RESULT_MESSAGE, null)
        )
    }
}
