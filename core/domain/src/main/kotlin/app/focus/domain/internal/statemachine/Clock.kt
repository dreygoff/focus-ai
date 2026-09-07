package app.focus.domain.internal.statemachine

interface Clock {
    fun nowMillis(): Long
}

class RealClock : Clock {
    override fun nowMillis() = System.currentTimeMillis()
}
