package app.focus.data

import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ProfileDao
import app.focus.data.mapper.toDomain
import app.focus.data.mapper.toEntity as toDomainToEntity
import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealProfileRepository(
    private val profileDao: ProfileDao,
    private val profileAppDao: ProfileAppDao
) : ProfileRepository {

    companion object {
        const val DEFAULT_SORT = 100
        private const val SETTINGS_PREFS = "core_settings"
        private const val KEY_DEFAULT_PROFILE = "default_profile_id"
    }

    override suspend fun insert(profile: app.focus.domain.model.Profile): Long {
        val entity = profile.toDomainToEntity()
        return profileDao.insert(entity)
    }

    override suspend fun update(profile: app.focus.domain.model.Profile) {
        val entity = profile.toDomainToEntity(isNew = false)
        profileDao.update(entity)
    }

    override suspend fun delete(id: String) {
        profileDao.getById(id)?.let { profileDao.delete(it) }
    }

    override fun observeProfiles(): Flow<List<app.focus.domain.model.Profile>> {
        return profileDao.observeProfiles()
            .map { entities -> entities.mapNotNull { it.toDomain() } }
    }

    override suspend fun getProfile(id: String): app.focus.domain.model.Profile? {
        val packages = profileAppDao.getPackageNames(id)
        return profileDao.getById(id)?.toDomain()
            ?: run {
                app.focus.domain.model.Profile(
                    id = id, name = "Loaded Profile", emoji = null, colorArgb = 0xFF2F6F6D.toInt(),
                    lockMode = LockMode.Soft, defaultDurationMinutes = 25, bypassDelaySeconds = 30,
                    bypassBreathingEnabled = true, bypassReasonRequired = true, bypassPhrase = null,
                    bypassLimitPerSession = 3, accessWindowMinutes = 5, bypassAppliesToAllApps = false,
                    emergencyExitMode = app.focus.domain.model.EmergencyExitMode.DELAY_10_MIN,
                    blockNewApps = true, deviceAdminProtection = false, allowedSettingsShortcuts = emptySet(),
                    hideTargetNotifications = false, targetPackageNames = packages,
                    createdAt = System.currentTimeMillis() - 86400000L, updatedAt = System.currentTimeMillis(),
                    sortOrder = DEFAULT_SORT
                )
            }
    }

    override suspend fun getDefaultProfileId(): String? = null
    override suspend fun setDefaultProfileId(id: String) {}
}
