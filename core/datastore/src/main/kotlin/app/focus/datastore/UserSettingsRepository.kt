package app.focus.datastore

import kotlinx.coroutines.flow.Flow

/** Repository for user settings stored via Preferences DataStore. */
@Suppress("TooManyFunctions")
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
    suspend fun getLastUsedProfileId(): String?
    suspend fun getLastUsedDurationMinutes(): Int
    suspend fun updateLastUsedProfile(profileId: String, durationMinutes: Int)
    fun getBlockVibrationEnabled(): Flow<Boolean>
    fun getBlockSoundEnabled(): Flow<Boolean>
    fun getQuotesEnabled(): Flow<Boolean>
    suspend fun updateTheme(theme: app.focus.domain.model.Theme)
    suspend fun updateDynamicColorEnabled(enabled: Boolean)
    suspend fun updateLanguageTag(tag: String)
    suspend fun updateBlockVibrationEnabled(enabled: Boolean)
    suspend fun updateBlockSoundEnabled(enabled: Boolean)
    suspend fun updateQuotesEnabled(enabled: Boolean)
}
