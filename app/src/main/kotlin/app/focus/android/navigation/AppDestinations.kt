package app.focus.android.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes per §11.1.
 */
sealed interface AppRoute {
    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object MainGraph : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data class StartSession(val profileId: String? = null) : AppRoute

    @Serializable
    data object ActiveSession : AppRoute

    @Serializable
    data class SessionSummary(val sessionId: String) : AppRoute

    @Serializable
    data object Profiles : AppRoute

    @Serializable
    data class ProfileEditor(val profileId: String? = null) : AppRoute

    @Serializable
    data class AppPicker(val profileId: String, val isFromProfile: Boolean = true) : AppRoute

    @Serializable
    data object Schedules : AppRoute

    @Serializable
    data class ScheduleEditor(val scheduleId: String? = null) : AppRoute

    @Serializable
    data object Stats : AppRoute

    @Serializable
    data object EventLog : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object PermissionsScreen : AppRoute

    @Serializable
    data object Allowlist : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object HowProtectionWorks : AppRoute
}

// Feature-specific navigation routes with arguments
object NavArgs {
    const val PROFILE_ID = "profileId"
    const val SCHEDULE_ID = "scheduleId"
    const val SESSION_ID = "sessionId"
    const val IS_FROM_PROFILE = "isFromProfile"

    val profileArg = navArgument(PROFILE_ID) { type = NavType.StringType; nullable = true }
    val scheduleArg = navArgument(SCHEDULE_ID) { type = NavType.StringType; nullable = true }
    val sessionArg = navArgument(SESSION_ID) { type = NavType.StringType }
}
