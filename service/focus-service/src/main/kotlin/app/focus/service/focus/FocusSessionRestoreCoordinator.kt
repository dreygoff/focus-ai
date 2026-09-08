package app.focus.service.focus

import android.content.Intent
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class FocusSessionRestoreCoordinator(
    private val scope: CoroutineScope,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val alarmScheduler: AlarmSchedulerService,
    private val onRestored: (sessionId: String?, profileName: String, plannedEndAtMillis: Long) -> Unit,
) {
    fun restore(intent: Intent) {
        scope.launch {
            val snapshot = snapshotStore.load()
            val sessionId = intent.getStringExtra(FocusForegroundService.EXTRA_SESSION_ID) ?: snapshot?.sessionId
            val profileName = intent.getStringExtra(FocusForegroundService.EXTRA_PROFILE_NAME)
                .orEmpty()
                .ifBlank { "Focus" }
            val plannedEndAt = intent.getLongExtra(
                FocusForegroundService.EXTRA_PLANNED_END_AT,
                snapshot?.plannedEndAtMillis ?: 0L,
            )

            snapshot?.let { loaded ->
                alarmScheduler.scheduleExact(
                    alarmMillis = loaded.plannedEndAtMillis,
                    operationCode = loaded.sessionId.hashCode(),
                    receiverClassName = AlarmReceiver::class.java.name,
                    sessionId = loaded.sessionId,
                )
            }
            onRestored(sessionId, profileName, plannedEndAt)
        }
    }
}
