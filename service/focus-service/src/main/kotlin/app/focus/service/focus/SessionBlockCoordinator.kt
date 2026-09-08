package app.focus.service.focus

import android.util.Log
import app.focus.domain.model.BlockDecision
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.model.Session
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.BlockLauncher
import app.focus.domain.usecase.BlockRequest
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.DecideBlockUseCase
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.SessionRepository
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
    val blockState: ActiveSessionBlockState,
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
    ) {
        if (packageName.isBlank() || packageName == deps.appPackageName) return

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

    private suspend fun showBlock(request: ForegroundBlockRequest) {
        val session = deps.sessionRepository.observeActiveSession().first() ?: return
        val now = deps.clock.nowMillis()
        val latencyMs = (now - request.detectedAtMillis).coerceAtLeast(0)

        logEvent(session, EventType.BLOCK_LATENCY_MS, request.packageName, latencyMs.toString())

        val remainingMillis = (request.plannedEndAtMillis - now).coerceAtLeast(0)
        deps.blockLauncher.show(
            BlockRequest(
                sessionId = session.id,
                profileId = session.profileId,
                profileName = request.profileName,
                blockedPackage = request.packageName,
                blockedAppName = request.appName.ifBlank { request.packageName },
                lockMode = session.lockMode,
                remainingMillis = remainingMillis,
                attemptNumber = session.blockAttempts + 1,
                bypassesUsed = session.bypassesUsed,
            ),
        )

        deps.sessionRepository.update(session.copy(blockAttempts = session.blockAttempts + 1))
        logEvent(session, EventType.BLOCK_SHOWN, request.packageName, request.decision.reason.name)

        Log.d(TAG, "Blocked ${request.packageName} (${latencyMs}ms latency)")
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
    }
}
