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

    companion object {
        private const val DEFAULT_RETENTION_DAYS = 90L

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
