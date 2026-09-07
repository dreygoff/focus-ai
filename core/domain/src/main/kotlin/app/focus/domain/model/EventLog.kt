package app.focus.domain.model

/**
 * Domain model representing an event log entry.
 */
data class EventLog(
    val id: Long = 0,
    val timestamp: Long,
    val sessionId: String?,
    val type: EventType,
    val packageName: String?,
    val payload: String?
)
