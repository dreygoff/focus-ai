package app.focus.feature.session.routes

object SessionRoutes {
    const val START_SESSION = "session/start?profileId={profileId}&durationMinutes={durationMinutes}"
    const val ARG_PROFILE_ID = "profileId"
    const val ARG_DURATION_MINUTES = "durationMinutes"
    const val ACTIVE_SESSION = "session/active/{sessionId}"
    const val SESSION_SUMMARY = "session/summary/{sessionId}"

    fun start(profileId: String? = null, durationMinutes: Int = 25): String =
        "session/start?profileId=${profileId.orEmpty()}&durationMinutes=$durationMinutes"

    fun activeSession(sessionId: String) = "session/active/$sessionId"
    fun sessionSummary(sessionId: String) = "session/summary/$sessionId"
}
