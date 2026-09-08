package app.focus.domain.usecase

import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ClearStatisticsUseCase(
    private val statsRepository: StatsRepository,
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        statsRepository.clearStatistics()
    }
}

class DeleteAllUserDataUseCase(
    private val deps: Dependencies,
) {
    data class Dependencies(
        val statsRepository: StatsRepository,
        val profileRepository: ProfileRepository,
        val scheduleRepository: ScheduleRepository,
        val allowlistRepository: AllowlistRepository,
        val sessionRepository: SessionRepository,
        val snapshotStore: ActiveSessionSnapshotStorage,
        val reseedProfiles: suspend () -> Unit,
    )

    suspend fun execute() = withContext(Dispatchers.IO) {
        val active = deps.sessionRepository.observeActiveSession().first()
        if (active != null && active.status.isActive()) {
            error("Cannot delete data during an active session")
        }
        if (active?.lockMode is LockMode.Hard) {
            error("Cannot delete data during hard lock")
        }

        deps.statsRepository.clearStatistics()
        deps.scheduleRepository.observeSchedules().first().forEach { deps.scheduleRepository.delete(it.id) }
        deps.profileRepository.observeProfiles().first().forEach { deps.profileRepository.delete(it.id) }
        deps.allowlistRepository.observeAllowlist().first()
            .filter { pkg -> pkg !in SYSTEM_PACKAGES }
            .forEach { pkg -> deps.allowlistRepository.removePackage(pkg) }
        deps.snapshotStore.clear()
        deps.reseedProfiles()
    }

    private fun SessionStatus.isActive(): Boolean =
        this is SessionStatus.Running || this is SessionStatus.Paused

    companion object {
        private val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "app.focus.android",
        )
    }
}
