package app.focus.domain.model

/**
 * Event types for session logging and analytics (internal).
 * Corresponds to event_log.type column in database.
 */
enum class EventType {
    SESSION_STARTED,
    SESSION_PAUSED,
    SESSION_RESUMED,
    SESSION_COMPLETED,
    SESSION_CANCELLED,
    SESSION_EXPIRED,
    BLOCK_SHOWN,
    BYPASS_STARTED,
    BYPASS_GRANTED,
    BYPASS_DENIED,
    ACCESS_WINDOW_EXPIRED,
    TAMPER_ATTEMPT,
    EMERGENCY_EXIT_REQUESTED,
    EMERGENCY_EXIT_COMPLETED,
    PERMISSION_LOST,
    SERVICE_RESTARTED,
    BLOCK_LATENCY_MS;

    companion object {
        fun fromString(value: String): EventType? = entries.find { it.name == value }
        fun toString(eventType: EventType): String = eventType.name
    }
}
