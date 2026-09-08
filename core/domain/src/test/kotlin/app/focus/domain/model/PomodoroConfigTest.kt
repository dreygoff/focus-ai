package app.focus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit tests for PomodoroConfig phase transitions and remaining time calculations. */
class PomodoroConfigTest {

    private lateinit var config: PomodoroConfig

    @Before
    fun setUp() {
        config = PomodoroConfig(
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            cyclesBeforeLongBreak = 4,
            totalCycles = 4,
        )
    }

    @Test
    fun `initial state should be FOCUS phase with full duration`() {
        assertEquals("FOCUS", config.currentPhaseName)
        assertEquals(25L * 60, config.currentPhaseRemainingSeconds())
    }

    @Test
    fun `default config should have standard pomodoro values`() {
        val defaultConfig = PomodoroConfig()
        assertEquals(25, defaultConfig.focusMinutes)
        assertEquals(5, defaultConfig.shortBreakMinutes)
        assertEquals(15, defaultConfig.longBreakMinutes)
        assertEquals(4, defaultConfig.cyclesBeforeLongBreak)
        assertEquals(4, defaultConfig.totalCycles)
    }

    @Test
    fun `after focus completes should switch to short break`() {
        val initialRemaining = config.currentPhaseRemainingSeconds()
        repeat(initialRemaining.toInt()) { config.tick() }
        assertEquals("SHORT_BREAK", config.currentPhaseName)
        assertEquals(5L * 60, config.currentPhaseRemainingSeconds())
    }

    @Test
    fun `isComplete should return false during active phase`() {
        assertFalse(config.isComplete())
    }

    @Test
    fun `isComplete should return true when all cycles complete`() {
        val testConfig = PomodoroConfig(
            focusMinutes = 1,
            shortBreakMinutes = 1,
            longBreakMinutes = 1,
            cyclesBeforeLongBreak = 2,
            totalCycles = 2,
        )
        val phaseDuration = testConfig.focusMinutes * 60L
        repeat(phaseDuration.toInt()) { testConfig.tick() }
        var remaining = testConfig.currentPhaseRemainingSeconds()
        repeat(remaining.toInt()) { testConfig.tick() }
        remaining = testConfig.currentPhaseRemainingSeconds()
        repeat(remaining.toInt()) { testConfig.tick() }
        assertTrue(testConfig.isComplete())
    }

    @Test
    fun `remainingFocusCycles should count down correctly`() {
        assertEquals(4, config.remainingFocusCycles())
        val focusDuration = config.focusMinutes * 60L
        repeat(focusDuration.toInt()) { config.tick() }
        val remaining = config.currentPhaseRemainingSeconds()
        repeat(remaining.toInt()) { config.tick() }
        assertTrue(config.remainingFocusCycles() <= 3)
    }
}
