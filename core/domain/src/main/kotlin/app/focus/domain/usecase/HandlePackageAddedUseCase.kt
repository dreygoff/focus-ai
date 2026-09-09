package app.focus.domain.usecase

import app.focus.domain.model.LockMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class HandlePackageAddedUseCase(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val blockState: ActiveSessionBlockState,
) {
    sealed interface Result {
        data object NoActiveSession : Result
        data object AlreadyInProfile : Result
        data class AutoBlocked(val packageName: String) : Result
        data class PromptToAdd(
            val packageName: String,
            val profileId: String,
            val sessionId: String,
        ) : Result
    }

    suspend fun execute(packageName: String): Result = withContext(Dispatchers.IO) {
        val session = sessionRepository.observeActiveSession().firstOrNull()
            ?: return@withContext Result.NoActiveSession
        if (packageName in session.targetPackagesSnapshot) {
            return@withContext Result.AlreadyInProfile
        }

        val profile = profileRepository.getProfile(session.profileId)
            ?: return@withContext Result.NoActiveSession

        if (profile.lockMode is LockMode.Hard && profile.blockNewApps) {
            addPackageToActiveSession(session.profileId, session.id, packageName)
            return@withContext Result.AutoBlocked(packageName)
        }

        Result.PromptToAdd(
            packageName = packageName,
            profileId = session.profileId,
            sessionId = session.id,
        )
    }

    suspend fun addPackageToActiveProfile(packageName: String): Result = withContext(Dispatchers.IO) {
        val session = sessionRepository.observeActiveSession().firstOrNull()
            ?: return@withContext Result.NoActiveSession
        if (packageName in session.targetPackagesSnapshot) {
            return@withContext Result.AlreadyInProfile
        }
        addPackageToActiveSession(session.profileId, session.id, packageName)
        Result.AutoBlocked(packageName)
    }

    private suspend fun addPackageToActiveSession(
        profileId: String,
        sessionId: String,
        packageName: String,
    ) {
        val profile = profileRepository.getProfile(profileId) ?: return
        val updatedProfilePackages = (profile.targetPackageNames + packageName).distinct()
        profileRepository.updateTargetApps(profileId, updatedProfilePackages)

        val session = sessionRepository.observeActiveSession().firstOrNull()
        val snapshot = snapshotStore.load()?.takeIf { it.sessionId == sessionId }
        if (session == null || snapshot == null) return

        val updatedTargets = (session.targetPackagesSnapshot + packageName).distinct()
        sessionRepository.update(session.copy(targetPackagesSnapshot = updatedTargets))

        val updatedSnapshot = snapshot.copy(targetPackages = updatedTargets)
        snapshotStore.save(updatedSnapshot)
        val isPaused = session.status is app.focus.domain.model.SessionStatus.Paused
        blockState.updateFromSnapshot(updatedSnapshot, isPaused = isPaused)
    }
}
