package app.focus.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.focus.android.di.MaintenanceEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class DailyMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MaintenanceEntryPoint::class.java,
            )
            entryPoint.recalculateDailyStatsUseCase().execute()
            entryPoint.pruneEventLogUseCase().execute()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_maintenance"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
