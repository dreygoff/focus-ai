package app.focus.domain.usecase

interface ScheduleSkipRepository {
    suspend fun skipToday(scheduleId: String)
    suspend fun isSkippedToday(scheduleId: String): Boolean
    suspend fun clearSkip(scheduleId: String)
}
