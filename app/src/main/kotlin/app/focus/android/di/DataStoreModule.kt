package app.focus.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.focus.datastore.UserSettingsRepository
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("user_settings") }
    )

    @Provides
    @Singleton
    fun provideActiveSessionSnapshotStorage(
        @ApplicationContext context: Context
    ): ActiveSessionSnapshotStorage {
        return DeviceProtectedSessionSnapshotStorage(context)
    }

    /**
     * Provides a UserSettingsRepository implementation.
     */
    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>
    ): UserSettingsRepository {
        return RealUserSettingsRepository(dataStore)
    }
}

/**
 * Device-protected storage implementation for ActiveSessionSnapshotStorage.
 * For Direct Boot compatibility, data is stored in the device-protected directory.
 */
class DeviceProtectedSessionSnapshotStorage(
    private val context: android.content.Context
) : ActiveSessionSnapshotStorage {

    companion object {
        private const val SNAPSHOT_PREFS = "focus_active_session_snapshot"
    }

    override suspend fun save(snapshot: app.focus.domain.internal.statemachine.SessionSnapshot) {
        val deviceContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("session_id", snapshot.sessionId)
            putString("lock_mode", snapshot.lockMode)
            putLong("planned_end_at_millis", snapshot.plannedEndAtMillis)
            putString("target_packages", snapshot.targetPackages.joinToString("|"))
            putString("hard_lock_extra_packages", snapshot.hardLockExtraPackages.joinToString("|"))
            putString("default_launcher_pkg", snapshot.defaultLauncherPkg ?: "")
            putBoolean("is_pomodoro", snapshot.isPomodoro)
            putString("current_phase", snapshot.currentPhase ?: "FOCUS")
            putLong("phase_end_at_millis", snapshot.phaseEndAtMillis)
        }.apply()
    }

    override suspend fun load(): app.focus.domain.internal.statemachine.SessionSnapshot? {
        val deviceContext = context.createDeviceProtectedStorageContext()
        val prefs = deviceContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
        val sessionId = prefs.getString("session_id", null) ?: return null
        if (sessionId.isBlank()) return null

        return app.focus.domain.internal.statemachine.SessionSnapshot(
            sessionId = sessionId,
            lockMode = prefs.getString("lock_mode", "SOFT") ?: "SOFT",
            plannedEndAtMillis = prefs.getLong("planned_end_at_millis", 0L),
            targetPackages = prefs.getString("target_packages", "")?.split("|")?.filter { it.isNotBlank() }?.toList() ?: emptyList(),
            hardLockExtraPackages = prefs.getString("hard_lock_extra_packages", "")?.split("|")?.filter { it.isNotBlank() }?.toList() ?: emptyList(),
            defaultLauncherPkg = prefs.getString("default_launcher_pkg", null)?.takeIf { it.isNotBlank() },
            isPomodoro = prefs.getBoolean("is_pomodoro", false),
            currentPhase = prefs.getString("current_phase", "FOCUS") ?: "FOCUS",
            phaseEndAtMillis = prefs.getLong("phase_end_at_millis", 0L)
        )
    }

    override suspend fun clear() {
        val deviceContext = context.createDeviceProtectedStorageContext()
        deviceContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/**
 * RealUserSettingsRepository backed by Preferences DataStore.
 */
class RealUserSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    companion object {
        val THEME_KEY = longPreferencesKey("theme")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_TAG_KEY = stringPreferencesKey("language_tag")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    override fun getTheme(): kotlinx.coroutines.flow.Flow<app.focus.domain.model.Theme> {
        return dataStore.data.map { prefs ->
            when (prefs[THEME_KEY]) {
                1L -> app.focus.domain.model.Theme.LIGHT
                2L -> app.focus.domain.model.Theme.DARK
                else -> app.focus.domain.model.Theme.SYSTEM
            }
        }
    }

    override fun getDynamicColorEnabled(): kotlinx.coroutines.flow.Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[DYNAMIC_COLOR_KEY] ?: true }

    override fun getLanguageTag(): kotlinx.coroutines.flow.Flow<String> =
        dataStore.data.map { prefs -> prefs[LANGUAGE_TAG_KEY] ?: "ru" }

    override suspend fun isOnboardingCompleted(): Boolean =
        dataStore.data.first()[ONBOARDING_COMPLETED_KEY] == true

    override suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = true
        }
    }

    override suspend fun isAccessibilityDisclosureAccepted(): Boolean = false
    override suspend fun acceptAccessibilityDisclosure() {}
    override fun getEventLogRetentionDays(): kotlinx.coroutines.flow.Flow<Long> = kotlinx.coroutines.flow.MutableStateFlow(90L)
    override suspend fun updateEventLogRetentionDays(days: Long) {}
}
