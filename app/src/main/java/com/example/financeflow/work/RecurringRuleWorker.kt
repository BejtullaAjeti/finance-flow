package com.example.financeflow.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.recurring.RecurringRuleProcessor
import java.util.concurrent.TimeUnit

class RecurringRuleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RecurringRuleProcessor(AppDatabase.getInstance(applicationContext)).generateDueTransactions()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "recurring_rule_check"

        // ponytail: rules are date-granularity, not time-of-day, so checking twice a day is
        // plenty to catch the midnight rollover promptly without extra battery/Doze impact.
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringRuleWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
