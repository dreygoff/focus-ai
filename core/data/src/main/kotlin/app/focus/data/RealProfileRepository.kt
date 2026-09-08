package app.focus.data

import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ProfileDao
import app.focus.data.mapper.toDomain
import app.focus.data.mapper.toEntity as toDomainToEntity
import app.focus.data.mapper.toJsonList
import app.focus.domain.usecase.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealProfileRepository(
    private val profileDao: ProfileDao,
    private val profileAppDao: ProfileAppDao
) : ProfileRepository {

    companion object {
        const val DEFAULT_SORT = 100
    }

    private var defaultProfileId: String? = null

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
        val entity = profileDao.getById(id) ?: return null
        val packages = profileAppDao.getPackageNames(id)
        val profile = entity.toDomain()
        return profile.copy(
            targetPackageNames = packages.ifEmpty { profile.targetPackageNames },
        )
    }

    override suspend fun getDefaultProfileId(): String? = defaultProfileId

    override suspend fun setDefaultProfileId(id: String) {
        defaultProfileId = id
    }

    override suspend fun updateTargetApps(profileId: String, packageNames: List<String>) {
        profileAppDao.deleteAllForProfile(profileId)
        val now = System.currentTimeMillis()
        packageNames.forEach { pkg ->
            profileAppDao.insert(
                app.focus.database.entity.ProfileAppEntity(
                    profileId = profileId,
                    packageName = pkg,
                    addedAt = now,
                ),
            )
        }
        profileDao.getById(profileId)?.let { entity ->
            profileDao.update(
                entity.copy(
                    targetPackageNames = packageNames.toJsonList(),
                    updatedAt = now,
                ),
            )
        }
    }
}
