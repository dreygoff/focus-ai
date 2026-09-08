package app.focus.data

import android.content.Context
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.ScheduleSkipRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsScheduleSkipRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
) : ScheduleSkipRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun skipToday(scheduleId: String) {
        prefs.edit().putString(keyFor(scheduleId), todayKey()).apply()
    }

    override suspend fun isSkippedToday(scheduleId: String): Boolean {
        return prefs.getString(keyFor(scheduleId), null) == todayKey()
    }

    override suspend fun clearSkip(scheduleId: String) {
        prefs.edit().remove(keyFor(scheduleId)).apply()
    }

    private fun keyFor(scheduleId: String): String = "skip_$scheduleId"

    private fun todayKey(): String =
        DateTimeFormatter.ofPattern("yyyyMMdd")
            .format(Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()))

    companion object {
        private const val PREFS_NAME = "schedule_skips"
    }
}
