package app.focus.android.datastore

/**
 * Repository interface for user settings stored in Proto DataStore.
 */
interface UserSettingsRepository {
    /** Get the current theme setting. */
    fun getTheme(): kotlinx.coroutines.flow.Flow<Theme>

    /** Get whether dynamic color is enabled. */
    fun getDynamicColorEnabled(): kotlinx.coroutines.flow.Flow<Boolean>

    /** Get the language tag. */
    fun getLanguageTag(): kotlinx.coroutines.flow.Flow<String>

    /** Check if onboarding has been completed. */
    suspend fun isOnboardingCompleted(): Boolean

    /** Mark onboarding as completed. */
    suspend fun completeOnboarding()

    /** Check if accessibility disclosure has been accepted. */
    suspend fun isAccessibilityDisclosureAccepted(): Boolean

    /** Mark accessibility disclosure as accepted. */
    suspend fun acceptAccessibilityDisclosure()

    /** Get event log retention days. */
    fun getEventLogRetentionDays(): kotlinx.coroutines.flow.Flow<Long>

    /** Update event log retention days. */
    suspend fun updateEventLogRetentionDays(days: Long)

    enum class Theme {
        SYSTEM, LIGHT, DARK
    }
}
