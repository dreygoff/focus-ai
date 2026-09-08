package app.focus.datastore

import kotlinx.coroutines.flow.Flow

/** Repository for user settings stored via Preferences DataStore. */
interface UserSettingsRepository {
    fun getTheme(): Flow<app.focus.domain.model.Theme>
    fun getDynamicColorEnabled(): Flow<Boolean>
    fun getLanguageTag(): Flow<String>
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun completeOnboarding()
    suspend fun isAccessibilityDisclosureAccepted(): Boolean
    suspend fun acceptAccessibilityDisclosure()
    fun getEventLogRetentionDays(): Flow<Long>
    suspend fun updateEventLogRetentionDays(days: Long)
}
