package app.focus.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import app.focus.android.datastore.UserSettings as UserSettingsProto
import app.focus.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

/**
 * Proto DataStore-backed implementation of [UserSettingsRepository] (§9.2).
 */
@Suppress("TooManyFunctions")
class ProtoUserSettingsRepository(
    private val dataStore: DataStore<UserSettingsProto>,
) : UserSettingsRepository {

    override fun getTheme(): Flow<Theme> =
        dataStore.data.map { proto -> proto.theme.toDomain() }

    override fun getDynamicColorEnabled(): Flow<Boolean> =
        dataStore.data.map { it.dynamicColor }

    override fun getLanguageTag(): Flow<String> =
        dataStore.data.map { it.languageTag.ifBlank { "ru" } }

    override suspend fun isOnboardingCompleted(): Boolean =
        dataStore.data.first().onboardingCompleted

    override suspend fun completeOnboarding() {
        dataStore.updateData { current ->
            current.toBuilder().setOnboardingCompleted(true).build()
        }
    }

    override suspend fun isAccessibilityDisclosureAccepted(): Boolean =
        dataStore.data.first().accessibilityDisclosureAccepted

    override suspend fun acceptAccessibilityDisclosure() {
        dataStore.updateData { current ->
            current.toBuilder().setAccessibilityDisclosureAccepted(true).build()
        }
    }

    override fun getEventLogRetentionDays(): Flow<Long> =
        dataStore.data.map { it.eventLogRetentionDays.takeIf { days -> days > 0 } ?: DEFAULT_RETENTION_DAYS }

    override suspend fun updateEventLogRetentionDays(days: Long) {
        dataStore.updateData { current ->
            current.toBuilder().setEventLogRetentionDays(days).build()
        }
    }

    override suspend fun getLastUsedProfileId(): String? =
        dataStore.data.first().lastUsedProfileId.takeIf { it.isNotBlank() }

    override suspend fun getLastUsedDurationMinutes(): Int {
        val minutes = dataStore.data.first().lastUsedDurationMinutes
        return minutes.takeIf { it > 0 } ?: DEFAULT_DURATION_MINUTES
    }

    override suspend fun updateLastUsedProfile(profileId: String, durationMinutes: Int) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setLastUsedProfileId(profileId)
                .setLastUsedDurationMinutes(durationMinutes)
                .build()
        }
    }

    override fun getBlockVibrationEnabled(): Flow<Boolean> =
        dataStore.data.map { it.blockVibration }

    override fun getBlockSoundEnabled(): Flow<Boolean> =
        dataStore.data.map { it.blockSound }

    override fun getQuotesEnabled(): Flow<Boolean> =
        dataStore.data.map { it.quotesEnabled }

    override suspend fun updateTheme(theme: Theme) {
        dataStore.updateData { current ->
            current.toBuilder().setTheme(theme.toProto()).build()
        }
    }

    override suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder().setDynamicColor(enabled).build()
        }
    }

    override suspend fun updateLanguageTag(tag: String) {
        dataStore.updateData { current ->
            current.toBuilder().setLanguageTag(tag).build()
        }
    }

    override suspend fun updateBlockVibrationEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder().setBlockVibration(enabled).build()
        }
    }

    override suspend fun updateBlockSoundEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder().setBlockSound(enabled).build()
        }
    }

    override suspend fun updateQuotesEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder().setQuotesEnabled(enabled).build()
        }
    }

    companion object {
        private const val DEFAULT_RETENTION_DAYS = 90L
        private const val DEFAULT_DURATION_MINUTES = 25

        fun create(context: android.content.Context): ProtoUserSettingsRepository {
            val dataStore = DataStoreFactory.create(
                serializer = UserSettingsSerializer,
                produceFile = { context.filesDir.resolve("datastore/user_settings.pb") },
            )
            return ProtoUserSettingsRepository(dataStore)
        }
    }
}

private object UserSettingsSerializer : Serializer<UserSettingsProto> {
    override val defaultValue: UserSettingsProto = UserSettingsProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserSettingsProto =
        UserSettingsProto.parseFrom(input)

    override suspend fun writeTo(t: UserSettingsProto, output: OutputStream) {
        t.writeTo(output)
    }
}

private fun UserSettingsProto.Theme.toDomain(): Theme = when (this) {
    UserSettingsProto.Theme.LIGHT -> Theme.LIGHT
    UserSettingsProto.Theme.DARK -> Theme.DARK
    UserSettingsProto.Theme.SYSTEM,
    UserSettingsProto.Theme.UNRECOGNIZED,
    -> Theme.SYSTEM
}

private fun Theme.toProto(): UserSettingsProto.Theme = when (this) {
    Theme.LIGHT -> UserSettingsProto.Theme.LIGHT
    Theme.DARK -> UserSettingsProto.Theme.DARK
    Theme.SYSTEM -> UserSettingsProto.Theme.SYSTEM
}
