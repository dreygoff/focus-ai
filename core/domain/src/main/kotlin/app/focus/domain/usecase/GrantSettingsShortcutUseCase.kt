package app.focus.domain.usecase

import app.focus.domain.model.SettingsShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GrantSettingsShortcutUseCase(
    private val accessWindowRepo: AccessWindowRepository,
    private val sessionRepo: SessionRepository,
    private val profileRepo: ProfileRepository,
    private val shortcutGateway: SettingsShortcutGateway,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock,
) {
    data class Result(
        val expiresAt: Long,
        val settingsPackage: String,
        val intentAction: String,
    )

    suspend fun execute(sessionId: String, shortcut: SettingsShortcut): Result = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: error("No active session")
        check(session.id == sessionId) { "Session mismatch" }
        check(session.lockMode is app.focus.domain.model.LockMode.Hard) { "Settings shortcuts only in hard lock" }

        val profile = profileRepo.getProfile(session.profileId)
            ?: error("Profile not found")
        check(shortcut in profile.allowedSettingsShortcuts) { "Shortcut not allowed for profile" }

        val target = shortcutGateway.resolve(shortcut)
            ?: error("Cannot resolve settings shortcut: $shortcut")

        val expiresAt = accessWindowRepo.grant(
            sessionId = sessionId,
            packageName = target.settingsPackage,
            reason = shortcut.name,
            durationMinutes = SETTINGS_SHORTCUT_MINUTES,
            restrictedToActivity = target.activityClassName,
        )

        scheduleAccessWindowAlarms(sessionId, target.settingsPackage, expiresAt)

        Result(
            expiresAt = expiresAt,
            settingsPackage = target.settingsPackage,
            intentAction = target.intentAction,
        )
    }

    private fun scheduleAccessWindowAlarms(sessionId: String, packageName: String, expiresAt: Long) {
        val warningAt = expiresAt - WARNING_BEFORE_EXPIRY_MS
        if (warningAt > clock.nowMillis()) {
            alarmScheduler.scheduleExact(
                alarmMillis = warningAt,
                operationCode = accessWindowWarningCode(sessionId, packageName),
                receiverClassName = ALARM_RECEIVER,
                sessionId = sessionId,
                action = ACTION_ACCESS_WINDOW_WARNING,
                extras = mapOf(KEY_PACKAGE_NAME to packageName),
            )
        }
        alarmScheduler.scheduleExact(
            alarmMillis = expiresAt,
            operationCode = accessWindowExpiryCode(sessionId, packageName),
            receiverClassName = ALARM_RECEIVER,
            sessionId = sessionId,
            action = ACTION_ACCESS_WINDOW_EXPIRED,
            extras = mapOf(KEY_PACKAGE_NAME to packageName),
        )
    }

    companion object {
        private const val SETTINGS_SHORTCUT_MINUTES = 1
        private const val WARNING_BEFORE_EXPIRY_MS = 30_000L
        const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"
        const val ACTION_ACCESS_WINDOW_WARNING = "app.focus.service.focus.ACTION_ACCESS_WINDOW_WARNING"
        const val ACTION_ACCESS_WINDOW_EXPIRED = "app.focus.service.focus.ACTION_ACCESS_WINDOW_EXPIRED"
        const val KEY_PACKAGE_NAME = "packageName"

        fun accessWindowWarningCode(sessionId: String, packageName: String): Int =
            "warn_${sessionId}_$packageName".hashCode()

        fun accessWindowExpiryCode(sessionId: String, packageName: String): Int =
            "exp_${sessionId}_$packageName".hashCode()
    }
}
