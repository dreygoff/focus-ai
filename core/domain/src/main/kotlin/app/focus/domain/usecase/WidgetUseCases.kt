package app.focus.domain.usecase

import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class WidgetMode {
    IDLE,
    ACTIVE,
    PAUSED,
    POMODORO_BREAK,
}

data class WidgetSessionState(
    val mode: WidgetMode,
    val profileName: String?,
    val remainingMillis: Long?,
    val canStop: Boolean,
)

class GetWidgetSessionStateUseCase(
    private val sessionRepository: SessionRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val clock: Clock,
) {
    suspend fun execute(): WidgetSessionState {
        val session = sessionRepository.observeActiveSession().first()
        if (session == null || !session.status.isActiveForWidget()) {
            return WidgetSessionState(WidgetMode.IDLE, null, null, canStop = false)
        }

        val snapshot = snapshotStore.load()
        val isPaused = session.status is SessionStatus.Paused
        val isBreak = snapshot?.isPomodoro == true && snapshot.currentPhase in BREAK_PHASES

        val mode = when {
            isBreak -> WidgetMode.POMODORO_BREAK
            isPaused -> WidgetMode.PAUSED
            else -> WidgetMode.ACTIVE
        }

        val endAtMillis = when {
            isBreak -> snapshot?.phaseEndAtMillis
            else -> session.plannedEndAt
        }
        val remaining = endAtMillis?.let { (it - clock.nowMillis()).coerceAtLeast(0L) }

        return WidgetSessionState(
            mode = mode,
            profileName = session.profileNameSnapshot,
            remainingMillis = remaining,
            canStop = session.lockMode is LockMode.Soft,
        )
    }

    private fun SessionStatus.isActiveForWidget(): Boolean =
        this is SessionStatus.Running || this is SessionStatus.Paused

    companion object {
        private val BREAK_PHASES = setOf("SHORT_BREAK", "LONG_BREAK", "BREAK")
    }
}

class QuickStartLastProfileUseCase(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val lastUsedProfileProvider: LastUsedProfileProvider,
    private val permissionsGateway: MandatoryPermissionsGateway,
) {
    sealed interface Result {
        data object Started : Result
        data object AlreadyActive : Result
        data object NoProfile : Result
        data object PermissionRequired : Result
    }

    suspend fun execute(): Result = withContext(Dispatchers.IO) {
        if (!permissionsGateway.areMandatoryGranted()) return@withContext Result.PermissionRequired
        if (sessionRepository.observeActiveSession().first() != null) {
            return@withContext Result.AlreadyActive
        }

        val profileId = lastUsedProfileProvider.profileId()
            ?: profileRepository.observeProfiles().first().firstOrNull()?.id
            ?: return@withContext Result.NoProfile

        startForProfile(profileId)
    }

    private suspend fun startForProfile(profileId: String): Result {
        val profile = profileRepository.getProfile(profileId) ?: return Result.NoProfile
        val durationMinutes = lastUsedProfileProvider.durationMinutes()
            .takeIf { minutes -> minutes > 0 }
            ?: profile.defaultDurationMinutes

        startSessionUseCase.execute(
            StartSessionRequest(
                profileId = profileId,
                durationMinutes = durationMinutes,
                goalText = null,
            ),
        )
        lastUsedProfileProvider.save(profileId, durationMinutes)
        return Result.Started
    }
}

class QuickStartProfileUseCase(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val lastUsedProfileProvider: LastUsedProfileProvider,
    private val permissionsGateway: MandatoryPermissionsGateway,
) {
    sealed interface Result {
        data object Started : Result
        data object AlreadyActive : Result
        data object NoProfile : Result
        data object PermissionRequired : Result
    }

    suspend fun execute(profileId: String): Result = withContext(Dispatchers.IO) {
        if (!permissionsGateway.areMandatoryGranted()) return@withContext Result.PermissionRequired
        if (sessionRepository.observeActiveSession().first() != null) {
            return@withContext Result.AlreadyActive
        }

        val profile = profileRepository.getProfile(profileId) ?: return@withContext Result.NoProfile
        val durationMinutes = profile.defaultDurationMinutes

        startSessionUseCase.execute(
            StartSessionRequest(
                profileId = profileId,
                durationMinutes = durationMinutes,
                goalText = null,
            ),
        )
        lastUsedProfileProvider.save(profileId, durationMinutes)
        Result.Started
    }
}

class QuickStopSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val stopSessionUseCase: StopSessionUseCase,
) {
    suspend fun execute(): Boolean = withContext(Dispatchers.IO) {
        val session = sessionRepository.observeActiveSession().first() ?: return@withContext false
        if (session.lockMode is LockMode.Hard) return@withContext false
        stopSessionUseCase.execute(session.id, SessionStatus.Cancelled)
        true
    }
}
