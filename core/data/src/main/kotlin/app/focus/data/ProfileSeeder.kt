package app.focus.data

import app.focus.database.dao.ProfileDao
import app.focus.database.entity.ProfileEntity
import kotlinx.coroutines.runBlocking

/**
 * Seeds preset profile templates on first database open (FR-13).
 */
class ProfileSeeder(
    private val profileDao: ProfileDao,
) {
    suspend fun seedIfEmpty() {
        if (profileDao.count() > 0) return
        defaultProfiles().forEach { profileDao.insert(it) }
    }

    companion object {
        const val ID_DEEP_WORK = "seed-deep-work"
        const val ID_SLEEP = "seed-sleep"
        const val ID_DETOX = "seed-detox"

        fun defaultProfiles(): List<ProfileEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                deepWorkProfile(now),
                sleepProfile(now),
                detoxProfile(now),
            )
        }

        fun seedIfEmptyBlocking(profileDao: ProfileDao) {
            runBlocking { ProfileSeeder(profileDao).seedIfEmpty() }
        }

        private fun deepWorkProfile(now: Long) = ProfileEntity(
            id = ID_DEEP_WORK,
            name = "Глубокая работа",
            emoji = "🧠",
            colorArgb = 0xFF2F6F6D.toInt(),
            lockMode = "SOFT",
            defaultDurationMinutes = 50,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = true,
            bypassReasonRequired = true,
            bypassPhrase = "Я осознанно отвлекаюсь от своей цели",
            bypassLimitPerSession = 3,
            accessWindowMinutes = 5,
            bypassAppliesToAllApps = false,
            emergencyExit = "DELAY_10_MIN",
            blockNewApps = true,
            deviceAdminProtection = false,
            allowedShortcuts = null,
            hideTargetNotifications = false,
            targetPackageNames = "[]",
            createdAt = now,
            updatedAt = now,
            sortOrder = 0,
        )

        private fun sleepProfile(now: Long) = ProfileEntity(
            id = ID_SLEEP,
            name = "Сон",
            emoji = "🌙",
            colorArgb = 0xFF1A237E.toInt(),
            lockMode = "HARD",
            defaultDurationMinutes = 480,
            bypassDelaySeconds = 0,
            bypassBreathingEnabled = false,
            bypassReasonRequired = false,
            bypassPhrase = null,
            bypassLimitPerSession = 0,
            accessWindowMinutes = 1,
            bypassAppliesToAllApps = false,
            emergencyExit = "NONE",
            blockNewApps = true,
            deviceAdminProtection = false,
            allowedShortcuts = null,
            hideTargetNotifications = false,
            targetPackageNames = "[]",
            createdAt = now,
            updatedAt = now,
            sortOrder = 1,
        )

        private fun detoxProfile(now: Long) = ProfileEntity(
            id = ID_DETOX,
            name = "Детокс",
            emoji = "📵",
            colorArgb = 0xFFBF360C.toInt(),
            lockMode = "HARD",
            defaultDurationMinutes = 120,
            bypassDelaySeconds = 0,
            bypassBreathingEnabled = false,
            bypassReasonRequired = false,
            bypassPhrase = null,
            bypassLimitPerSession = 0,
            accessWindowMinutes = 1,
            bypassAppliesToAllApps = false,
            emergencyExit = "DELAY_10_MIN",
            blockNewApps = true,
            deviceAdminProtection = false,
            allowedShortcuts = null,
            hideTargetNotifications = false,
            targetPackageNames = "[]",
            createdAt = now,
            updatedAt = now,
            sortOrder = 2,
        )
    }
}
