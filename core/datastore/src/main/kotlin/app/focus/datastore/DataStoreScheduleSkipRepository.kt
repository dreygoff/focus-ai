package app.focus.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.ScheduleSkipRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Context.scheduleSkipDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "schedule_skips",
)

class DataStoreScheduleSkipRepository(
    context: Context,
    private val clock: Clock,
) : ScheduleSkipRepository {
    private val dataStore = context.scheduleSkipDataStore

    override suspend fun skipToday(scheduleId: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(keyFor(scheduleId))] = todayKey()
        }
    }

    override suspend fun isSkippedToday(scheduleId: String): Boolean {
        val stored = dataStore.data.first()[stringPreferencesKey(keyFor(scheduleId))]
        return stored == todayKey()
    }

    override suspend fun clearSkip(scheduleId: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(keyFor(scheduleId)))
        }
    }

    private fun keyFor(scheduleId: String): String = "skip_$scheduleId"

    private fun todayKey(): String =
        DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .format(Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()))
}
