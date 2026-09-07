package app.focus.domain.model

sealed interface FocusEvent {
    data class Error(val message: String) : FocusEvent
    object NavigationRequired : FocusEvent

    data class EventWithPayload(val eventType: EventType) : FocusEvent
}
