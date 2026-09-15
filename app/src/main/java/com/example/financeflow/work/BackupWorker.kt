package com.example.financeflow.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.financeflow.R
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.backup.BackupDirtyTracker
import com.example.financeflow.data.backup.BackupFileStore
import com.example.financeflow.data.backup.BackupManager
import com.example.financeflow.data.backup.BackupTrigger
import com.example.financeflow.data.backup.toJson
import com.example.financeflow.locale.BackupFolderPreferences
import com.example.financeflow.locale.BackupResult
import java.util.concurrent.TimeUnit

/**
 * Writes an automatic backup into the configured folder (BackupFolderPreferences) — the
 * app-specific default folder it auto-selects on first launch, unless the user has since picked
 * a SAF folder from the Backups screen. Runs for two triggers, distinguished by input data: the
 * daily periodic job ([schedule]) and the app-close trigger ([triggerOnClose]), which
 * additionally skips writing if nothing has changed since the last backup of any kind
 * (BackupDirtyTracker).
 */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val folderUri = BackupFolderPreferences.ensureDefaultFolder(applicationContext)
        val trigger = runCatching {
            BackupTrigger.valueOf(inputData.getString(KEY_TRIGGER) ?: BackupTrigger.DAILY.name)
        }.getOrDefault(BackupTrigger.DAILY)

        if (trigger == BackupTrigger.ON_CLOSE && !BackupDirtyTracker.isDirty()) {
            // Nothing changed since the last backup (of any trigger) — skip the duplicate.
            return Result.success()
        }

        return try {
            val payload = BackupManager(AppDatabase.getInstance(applicationContext)).exportPayload()
            BackupFileStore.writeBackup(applicationContext, folderUri, payload.toJson(), trigger)
            BackupFileStore.enforceRetention(applicationContext, folderUri)
            BackupDirtyTracker.markClean()
            BackupFolderPreferences.recordResult(applicationContext, BackupResult(System.currentTimeMillis(), success = true, message = null))
            Result.success()
        } catch (e: Exception) {
            // Not transient (a revoked permission or a deleted folder won't fix itself on
            // retry) — Result.failure() plus the notification/banner is the real signal here,
            // not a retry loop.
            BackupFolderPreferences.recordResult(applicationContext, BackupResult(System.currentTimeMillis(), success = false, message = e.message))
            notifyFailure(applicationContext)
            Result.failure()
        }
    }

    private fun notifyFailure(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Not granted — the Backups screen's lastResultFlow banner is the fallback.
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.backup_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.backup_notification_failed_title))
            .setContentText(context.getString(R.string.backup_notification_failed_body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DAILY_WORK_NAME = "automatic_backup"
        private const val ON_CLOSE_WORK_NAME = "on_close_backup"
        private const val CHANNEL_ID = "backup_failures"
        private const val NOTIFICATION_ID = 1001
        private const val KEY_TRIGGER = "trigger"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(KEY_TRIGGER to BackupTrigger.DAILY.name))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Fire-and-forget from Activity.onStop() — enqueuing is itself non-blocking (posts to
         * WorkManager's own executor), and expedited scheduling runs this promptly instead of
         * waiting on Doze/battery-optimization deferral, since the app is heading to the
         * background and may not come back for a while. REPLACE means rapid background/
         * foreground cycles only ever leave the latest request queued, not a pile-up. */
        fun triggerOnClose(context: Context) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(workDataOf(KEY_TRIGGER to BackupTrigger.ON_CLOSE.name))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(ON_CLOSE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
