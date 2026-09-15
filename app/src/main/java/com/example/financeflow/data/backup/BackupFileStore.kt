package com.example.financeflow.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/** Why a given backup file exists — distinguishes the two automatic triggers from a manual one,
 * both for the Backups list label and for retention (only DAILY/ON_CLOSE are pruned). */
enum class BackupTrigger(val filePrefix: String) {
    DAILY("financeflow-auto-daily-"),
    ON_CLOSE("financeflow-auto-onclose-"),
    MANUAL("financeflow-manual-")
}

data class BackupFileInfo(
    val uri: Uri,
    val timestampMillis: Long,
    val trigger: BackupTrigger
)

private const val EXTENSION = ".json"

/**
 * Operations for the configured automatic-backup folder — distinct from [BackupManager]'s
 * payload export/import, which this builds on, and from the pre-existing manual "Export"/
 * "Import" on Settings (an arbitrary SAF location picked per export). Daily, on-close, and
 * manual ("Backup now" on this screen) backups all share the one configured folder but are
 * distinguished by filename prefix, so retention only ever prunes the two automatic triggers,
 * never manual ones.
 *
 * The folder is either a SAF tree (`content://`, picked via the folder picker — goes through
 * DocumentFile/ContentResolver) or the auto-selected default app-specific directory (`file://`,
 * see [com.example.financeflow.locale.BackupFolderPreferences.ensureDefaultFolder] — goes
 * through plain [File] I/O, since routing a `file://` URI through [DocumentFile.createFile]
 * would double up the `.json` extension via its MIME-type-based naming).
 */
object BackupFileStore {
    fun writeBackup(context: Context, folderUri: Uri, json: String, trigger: BackupTrigger): Uri {
        val fileName = "${trigger.filePrefix}${System.currentTimeMillis()}$EXTENSION"
        if (folderUri.scheme == "file") {
            val folder = File(folderUri.path ?: error("Invalid backup folder")).apply { mkdirs() }
            val file = File(folder, fileName)
            file.writeText(json)
            return Uri.fromFile(file)
        }
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: error("Backup folder is no longer accessible")
        val file = folder.createFile("application/json", fileName) ?: error("Could not create backup file")
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(json.toByteArray()) }
            ?: error("Could not open backup file for writing")
        return file.uri
    }

    fun listBackups(context: Context, folderUri: Uri): List<BackupFileInfo> {
        val entries: List<Pair<String, Uri>> = if (folderUri.scheme == "file") {
            File(folderUri.path ?: return emptyList()).listFiles()
                ?.map { it.name to Uri.fromFile(it) } ?: emptyList()
        } else {
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
            folder.listFiles().mapNotNull { file -> file.name?.let { it to file.uri } }
        }
        return entries.mapNotNull { (name, uri) ->
            val trigger = BackupTrigger.entries.firstOrNull { name.startsWith(it.filePrefix) } ?: return@mapNotNull null
            val timestamp = name.removePrefix(trigger.filePrefix).removeSuffix(EXTENSION).toLongOrNull() ?: return@mapNotNull null
            BackupFileInfo(uri = uri, timestampMillis = timestamp, trigger = trigger)
        }.sortedByDescending { it.timestampMillis }
    }

    fun readBackup(context: Context, uri: Uri): String =
        if (uri.scheme == "file") {
            File(uri.path ?: error("Invalid backup file")).readText()
        } else {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Could not open backup file")
        }

    fun deleteBackup(context: Context, uri: Uri) {
        if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() }
        } else {
            DocumentFile.fromSingleUri(context, uri)?.delete()
        }
    }

    /** Keeps the newest [keep] automatic (DAILY + ON_CLOSE, one merged pool) backups in the
     * folder, deleting older ones. Manual backups are never touched by this. Since eviction is
     * purely by recency across both triggers together, a same-day on-close backup sits right
     * next to that day's daily one at the top and isn't at any special risk of eviction — only
     * genuinely older backups from earlier days get dropped first. */
    fun enforceRetention(context: Context, folderUri: Uri, keep: Int = 10) {
        listBackups(context, folderUri)
            .filter { it.trigger != BackupTrigger.MANUAL }
            .drop(keep)
            .forEach { deleteBackup(context, it.uri) }
    }
}
