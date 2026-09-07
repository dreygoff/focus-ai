package app.focus.feature.session.routes

object SessionRoutes {
    const val START_SESSION = "session/start"
    const val ACTIVE_SESSION = "session/active/{sessionId}"
    const val SESSION_SUMMARY = "session/summary/{sessionId}"

    fun activeSession(sessionId: String) = "$ACTIVE_SESSION/$sessionId"
    fun sessionSummary(sessionId: String) = "$SESSION_SUMMARY/$sessionId"
}
