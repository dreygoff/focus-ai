package app.focus.android.di

import app.focus.datastore.UserSettingsRepository
import app.focus.domain.usecase.LastUsedProfileProvider
import app.focus.domain.usecase.MandatoryPermissionsGateway
import app.focus.domain.usecase.ProfileRepository
import app.focus.system.PermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetGatewayModule {

    @Binds
    @Singleton
    abstract fun bindMandatoryPermissionsGateway(
        gateway: AndroidMandatoryPermissionsGateway,
    ): MandatoryPermissionsGateway

    @Binds
    @Singleton
    abstract fun bindLastUsedProfileProvider(
        provider: DataStoreLastUsedProfileProvider,
    ): LastUsedProfileProvider
}

@Singleton
class AndroidMandatoryPermissionsGateway @Inject constructor(
    private val permissionChecker: PermissionChecker,
) : MandatoryPermissionsGateway {
    override fun areMandatoryGranted(): Boolean {
        permissionChecker.refreshPermissions()
        return permissionChecker.areMandatoryGranted()
    }
}

@Singleton
class DataStoreLastUsedProfileProvider @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val profileRepository: ProfileRepository,
) : LastUsedProfileProvider {
    override suspend fun profileId(): String? =
        userSettingsRepository.getLastUsedProfileId()
            ?: profileRepository.getDefaultProfileId()
            ?: profileRepository.observeProfiles().first().firstOrNull()?.id

    override suspend fun durationMinutes(): Int =
        userSettingsRepository.getLastUsedDurationMinutes()

    override suspend fun save(profileId: String, durationMinutes: Int) {
        userSettingsRepository.updateLastUsedProfile(profileId, durationMinutes)
        profileRepository.setDefaultProfileId(profileId)
    }
}
