package app.focus.service.focus

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FocusForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var deps: FocusServiceDependencies
    private lateinit var lifecycleHandler: FocusSessionLifecycleHandler
    private lateinit var restoreCoordinator: FocusSessionRestoreCoordinator
    private lateinit var blockCoordinator: SessionBlockCoordinator

    private var activeSessionId: String? = null
    private var profileName: String = "Focus"
    private var plannedEndAtMillis: Long = 0L
    private var detectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        deps = EntryPointAccessors.fromApplication(
            applicationContext,
            FocusServiceEntryPoint::class.java,
        ).dependencies()
        blockCoordinator = SessionBlockCoordinator(
            SessionBlockDependencies(
                decideBlockUseCase = deps.decideBlockUseCase,
                blockLauncher = deps.blockLauncher,
                eventLogRepository = deps.eventLogRepository,
                sessionRepository = deps.sessionRepository,
                blockState = deps.activeSessionBlockState,
                clock = deps.clock,
                appPackageName = packageName,
            ),
        )
        lifecycleHandler = FocusSessionLifecycleHandler(
            scope = serviceScope,
            stopSessionUseCase = deps.stopSessionUseCase,
            onRuntimeStopped = ::stopSessionRuntime,
        )
        restoreCoordinator = FocusSessionRestoreCoordinator(
            scope = serviceScope,
            snapshotStore = deps.snapshotStore,
            alarmScheduler = deps.alarmScheduler,
            onRestored = { sessionId, name, plannedEnd ->
                activeSessionId = sessionId
                profileName = name
                plannedEndAtMillis = plannedEnd
                refreshBlockState()
                promoteToForeground()
                startDetectors()
            },
        )
        createNotificationChannel()
        deps.detectorOrchestrator.registerUsageStatsPollingDetector(deps.usageStatsPollingDetector)
        deps.detectorOrchestrator.registerAccessibilityDetector(ForegroundEventBusAccessibilityDetector())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_RESTORE -> restoreCoordinator.restore(intent)
            ACTION_END_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: activeSessionId
                if (sessionId != null) lifecycleHandler.endSession(sessionId)
            }
            ACTION_USER_STOP, ACTION_STOP -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: activeSessionId
                if (sessionId == null) {
                    stopSessionRuntime()
                } else {
                    lifecycleHandler.cancelSession(sessionId)
                }
            }
            ACTION_SYNC_STOP -> stopSessionRuntime()
            ACTION_ACCESS_WINDOW_GRANTED -> handleAccessWindowGranted(intent)
            ACTION_ACCESS_WINDOW_EXPIRED -> handleAccessWindowExpired(intent)
            ACTION_EMERGENCY_EXIT_COMPLETE -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: activeSessionId
                if (sessionId != null) handleEmergencyExitComplete(sessionId)
            }
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        activeSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        profileName = intent.getStringExtra(EXTRA_PROFILE_NAME).orEmpty().ifBlank { "Focus" }
        plannedEndAtMillis = intent.getLongExtra(EXTRA_PLANNED_END_AT, 0L)
        refreshBlockState()
        promoteToForeground()
        startDetectors()
    }

    private fun refreshBlockState() {
        serviceScope.launch {
            val session = deps.sessionRepository.observeActiveSession().first()
            deps.snapshotStore.load()?.let { snapshot ->
                val isPaused = session?.status is app.focus.domain.model.SessionStatus.Paused
                deps.activeSessionBlockState.updateFromSnapshot(snapshot, isPaused)
                if (plannedEndAtMillis == 0L) {
                    plannedEndAtMillis = snapshot.plannedEndAtMillis
                }
            }
        }
    }

    private fun handleAccessWindowGranted(intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: return
        val appName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: packageName
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
        if (expiresAt > 0L) {
            AccessWindowNotificationFactory.showActive(this, packageName, appName, expiresAt)
        }
        blockCoordinator.dismissBlock()
    }

    private fun handleAccessWindowExpired(intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: return
        AccessWindowNotificationFactory.cancelAccessWindowNotification(this, packageName)
        serviceScope.launch {
            blockCoordinator.onForegroundApp(
                packageName = packageName,
                appName = packageName,
                detectedAtMillis = deps.clock.nowMillis(),
                profileName = profileName,
                plannedEndAtMillis = plannedEndAtMillis,
            )
        }
    }

    private fun promoteToForeground() {
        val notification = FocusSessionNotificationFactory.build(
            context = this,
            profileName = profileName,
            plannedEndAtMillis = plannedEndAtMillis,
            activeSessionId = activeSessionId,
        ).build()
        startForeground(NOTIF_ID, notification)
    }

    private fun startDetectors() {
        detectorJob?.cancel()
        detectorJob = serviceScope.launch {
            deps.detectorOrchestrator.events.collect { event ->
                blockCoordinator.onForegroundApp(
                    packageName = event.packageName,
                    appName = event.appName,
                    detectedAtMillis = event.timestampMillis,
                    profileName = profileName,
                    plannedEndAtMillis = plannedEndAtMillis,
                )
            }
        }
    }

    private fun stopSessionRuntime() {
        detectorJob?.cancel()
        deps.detectorOrchestrator.stopActiveDetectors()
        blockCoordinator.dismissBlock()
        activeSessionId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleEmergencyExitComplete(sessionId: String) {
        serviceScope.launch {
            runCatching { deps.completeEmergencyExitUseCase.execute(sessionId) }
            stopSessionRuntime()
        }
    }

    override fun onDestroy() {
        detectorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val ACTION_START = "app.focus.service.START"
        const val ACTION_STOP = "app.focus.service.STOP"
        const val ACTION_SYNC_STOP = "app.focus.service.SYNC_STOP"
        const val ACTION_USER_STOP = "app.focus.service.USER_STOP"
        const val ACTION_END_SESSION = "app.focus.service.END_SESSION"
        const val ACTION_RESTORE = "app.focus.service.RESTORE"
        const val ACTION_ACCESS_WINDOW_GRANTED = "app.focus.service.ACCESS_WINDOW_GRANTED"
        const val ACTION_ACCESS_WINDOW_EXPIRED = "app.focus.service.ACCESS_WINDOW_EXPIRED"
        const val ACTION_EMERGENCY_EXIT_COMPLETE = "app.focus.service.EMERGENCY_EXIT_COMPLETE"
        const val EXTRA_BLOCKED_PACKAGE = "blockedPackage"
        const val EXTRA_BLOCKED_APP_NAME = "blockedAppName"
        const val EXTRA_EXPIRES_AT = "expiresAt"
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_PROFILE_NAME = "profileName"
        const val EXTRA_PLANNED_END_AT = "plannedEndAt"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_SESSION_ID = "sessionId"
        const val NOTIF_ID = 1001
        const val CHANNEL_SESSION = "session"

        fun makeOpenPendingIntent(ctx: android.content.Context): PendingIntent {
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?: Intent().apply { setPackage(ctx.packageName) }
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getActivity(ctx, 9002, launchIntent, flags)
        }
    }
}
