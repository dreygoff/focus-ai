package app.focus.android.di

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.focus.domain.usecase.SessionRuntimeController
import app.focus.service.focus.FocusForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSessionRuntimeController @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionRuntimeController {

    override fun onSessionStarted(sessionId: String, profileName: String, plannedEndAtMillis: Long) {
        val intent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
            putExtra(FocusForegroundService.EXTRA_PROFILE_NAME, profileName)
            putExtra(FocusForegroundService.EXTRA_PLANNED_END_AT, plannedEndAtMillis)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun syncStopService() {
        val intent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_SYNC_STOP
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
