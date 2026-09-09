package app.focus.service.focus

import android.util.Log
import app.focus.domain.model.BlockDecision
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.model.Session
import app.focus.domain.usecase.BlockLauncher
import app.focus.domain.usecase.BlockRequest
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.DecideBlockUseCase
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.service.accessibility.TamperResponder
import app.focus.system.HardLockExtrasResolver
import kotlinx.coroutines.flow.first

internal data class ForegroundBlockRequest(
    val packageName: String,
    val appName: String,
    val detectedAtMillis: Long,
    val profileName: String,
    val plannedEndAtMillis: Long,
    val decision: BlockDecision.Block,
)

internal data class SessionBlockDependencies(
    val decideBlockUseCase: DecideBlockUseCase,
    val blockLauncher: BlockLauncher,
    val eventLogRepository: EventLogRepository,
    val sessionRepository: SessionRepository,
    val accessWindowRepository: app.focus.domain.usecase.AccessWindowRepository,
    val blockState: app.focus.domain.usecase.ActiveSessionBlockState,
    val clock: Clock,
    val appPackageName: String,
)

internal class SessionBlockCoordinator(
    private val deps: SessionBlockDependencies,
) {

    suspend fun onForegroundApp(
        packageName: String,
        appName: String,
        detectedAtMillis: Long,
        profileName: String,
        plannedEndAtMillis: Long,
        activityClassName: String? = null,
    ) {
        if (packageName.isBlank() || packageName == deps.appPackageName) return

        val tamperMessage = tamperMessageIfNeeded(packageName, activityClassName)
        if (tamperMessage != null) {
            handleTamperAttempt(
                packageName = packageName,
                appName = appName,
                detectedAtMillis = detectedAtMillis,
                profileName = profileName,
                plannedEndAtMillis = plannedEndAtMillis,
                tamperMessage = tamperMessage,
            )
            return
        }

        val decision = deps.decideBlockUseCase.decide(packageName)
        if (decision is BlockDecision.Block) {
            showBlock(
                ForegroundBlockRequest(
                    packageName = packageName,
                    appName = appName,
                    detectedAtMillis = detectedAtMillis,
                    profileName = profileName,
                    plannedEndAtMillis = plannedEndAtMillis,
                    decision = decision,
                ),
            )
        }
    }

    fun dismissBlock() {
        deps.blockLauncher.dismiss()
        deps.blockState.clear()
    }

    private suspend fun tamperMessageIfNeeded(
        packageName: String,
        activityClassName: String?,
    ): String? {
        val state = deps.blockState.sessionState
        if (!state.isHardLock) return null
        if (!HardLockExtrasResolver.isSettingsPackage(packageName)) return null

        val sessionId = state.sessionId ?: return TAMPER_MESSAGE
        val window = deps.accessWindowRepository.observeActiveWindows(sessionId).first()[packageName]
            ?: return TAMPER_MESSAGE

        val restricted = window.restrictedToActivity
        if (restricted != null && !activityClassName.isNullOrBlank()) {
            val allowed = activityClassName == restricted ||
                activityClassName.startsWith("$restricted$") ||
                restricted.endsWith(activityClassName.substringAfterLast('.'))
            if (!allowed) return TAMPER_MESSAGE
        }
        return null
    }

    private suspend fun handleTamperAttempt(
        packageName: String,
        appName: String,
        detectedAtMillis: Long,
        profileName: String,
        plannedEndAtMillis: Long,
        tamperMessage: String,
    ) {
        TamperResponder.respond()
        val session = deps.sessionRepository.observeActiveSession().first() ?: return
        logEvent(session, EventType.TAMPER_ATTEMPT, packageName, tamperMessage)
        showBlockWithMessage(
            session = session,
            packageName = packageName,
            appName = appName,
            detectedAtMillis = detectedAtMillis,
            profileName = profileName,
            plannedEndAtMillis = plannedEndAtMillis,
            tamperMessage = tamperMessage,
        )
        Log.d(TAG, "Tamper attempt blocked: $packageName")
    }

    private suspend fun showBlock(request: ForegroundBlockRequest) {
        val session = deps.sessionRepository.observeActiveSession().first() ?: return
        showBlockWithMessage(
            session = session,
            packageName = request.packageName,
            appName = request.appName,
            detectedAtMillis = request.detectedAtMillis,
            profileName = request.profileName,
            plannedEndAtMillis = request.plannedEndAtMillis,
            tamperMessage = null,
        )
        val latencyMs = (deps.clock.nowMillis() - request.detectedAtMillis).coerceAtLeast(0)
        logEvent(session, EventType.BLOCK_LATENCY_MS, request.packageName, latencyMs.toString())
        logEvent(session, EventType.BLOCK_SHOWN, request.packageName, request.decision.reason.name)
        Log.d(TAG, "Blocked ${request.packageName} (${latencyMs}ms latency)")
    }

    private suspend fun showBlockWithMessage(
        session: Session,
        packageName: String,
        appName: String,
        detectedAtMillis: Long,
        profileName: String,
        plannedEndAtMillis: Long,
        tamperMessage: String?,
    ) {
        val now = deps.clock.nowMillis()
        val remainingMillis = (plannedEndAtMillis - now).coerceAtLeast(0)
        deps.blockLauncher.show(
            BlockRequest(
                sessionId = session.id,
                profileId = session.profileId,
                profileName = profileName,
                blockedPackage = packageName,
                blockedAppName = appName.ifBlank { packageName },
                lockMode = session.lockMode,
                remainingMillis = remainingMillis,
                attemptNumber = session.blockAttempts + 1,
                bypassesUsed = session.bypassesUsed,
                tamperMessage = tamperMessage,
            ),
        )
        if (tamperMessage == null) {
            deps.sessionRepository.update(session.copy(blockAttempts = session.blockAttempts + 1))
        }
    }

    private suspend fun logEvent(
        session: Session,
        type: EventType,
        packageName: String,
        payload: String?,
    ) {
        runCatching {
            deps.eventLogRepository.log(
                EventLog(
                    timestamp = deps.clock.nowMillis(),
                    sessionId = session.id,
                    type = type,
                    packageName = packageName,
                    payload = payload,
                ),
            )
        }
    }

    companion object {
        private const val TAG = "SessionBlockCoordinator"
        private const val TAMPER_MESSAGE = "Settings are unavailable during this session"
    }
}
