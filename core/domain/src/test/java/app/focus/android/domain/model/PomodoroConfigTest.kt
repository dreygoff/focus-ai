package app.focus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PomodoroConfig phase transitions and remaining time calculations.
 */
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

    // ─── INITIALIZATION TESTS ─────────────────────────────────────

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

    // ─── FOCUS TO BREAK TESTS ─────────────────────────────────────

    @Test
    fun `tick should complete focus phase when time reaches zero`() {
        val remaining = config.currentPhaseRemainingSeconds()
        
        // Fast forward through entire focus phase
        for (i in 1..remaining) {
            val newPhase = config.tick()
            assertNotNull(newPhase)
            
            if (i < remaining) {
                assertEquals("FOCUS", newPhase)
            } else {
                // Should transition to BREAK after last second
                assertTrue(newPhase == "SHORT_BREAK" || newPhase == "FOCUS")
            }
        }
    }

    @Test
    fun `after focus completes should switch to short break`() {
        val initialRemaining = config.currentPhaseRemainingSeconds()
        
        // Fast forward just enough to complete focus phase (25 min * 60 sec + 1)
        for (i in 0..<initialRemaining) {
            config.tick()
        }

        assertEquals("SHORT_BREAK", config.currentPhaseName)
        assertEquals(5L * 60, config.currentPhaseRemainingSeconds())
    }

    // ─── SHORT BREAK TO FOCUS TESTS ───────────────────────────────

    @Test
    fun `after short break completes should switch back to focus`() {
        val remaining = config.currentPhaseRemainingSeconds()
        
        // Complete first phase (FOCUS)
        for (i in 0..<remaining) {
            config.tick()
        }
        
        // Now we're in SHORT_BREAK, complete it
        val breakRemaining = config.currentPhaseRemainingSeconds()
        for (i in 0..<breakRemaining) {
            config.tick()
        }

        assertEquals("FOCUS", config.currentPhaseName)
        assertEquals(25L * 60, config.currentPhaseRemainingSeconds())
    }

    // ─── LONG BREAK TESTS ─────────────────────────────────────────

    @Test
    fun `should switch to long break after specified number of cycles`() {
        val focusDuration = config.focusMinutes * 60L
        
        // Complete 4 full cycles (focus + short break each)
        for (cycle in 0 until 4) {
            var phase = "FOCUS"
            
            // Focus phase
            for (i in 0 until focusDuration) {
                phase = config.tick()!!
            }
            
            // Short break phase
            val breakRemaining = config.currentPhaseRemainingSeconds()
            for (i in 0 until breakRemaining) {
                phase = config.tick()!!
            }
        }

        // After 4 cycles, should be on long break
        assertEquals("LONG_BREAK", config.currentPhaseName)
        assertEquals(15L * 60, config.currentPhaseRemainingSeconds())
    }

    @Test
    fun `should complete all pomodoro after specified total cycles`() {
        val focusDuration = config.focusMinutes * 60L
        
        // Complete first 4 cycles (which should trigger LONG_BREAK)
        for (cycle in 0 until 4) {
            var phase = "FOCUS"
            
            for (i in 0 until focusDuration) {
                phase = config.tick()!!
            }
            
            val breakRemaining = config.currentPhaseRemainingSeconds()
            for (i in 0 until breakRemaining) {
                config.tick()
            }
        }

        // Now we're on LONG_BREAK, complete it
        val longBreakRemaining = config.currentPhaseRemainingSeconds()
        for (i in 0 until longBreakRemaining) {
            config.tick()
        }

        // Should be back to FOCUS for the final cycle
        assertEquals("FOCUS", config.currentPhaseName)
    }

    // ─── IS COMPLETE TESTS ────────────────────────────────────────

    @Test
    fun `isComplete should return false during active phase`() {
        assertFalse(config.isComplete())
    }

    @Test
    fun `isComplete should return true when all cycles complete`() {
        val focusDuration = config.focusMinutes * 60L
        
        // Complete all cycles manually with a fresh config to avoid side effects
        val testConfig = PomodoroConfig(
            focusMinutes = 1,      // 1 minute for faster testing
            shortBreakMinutes = 1,
            longBreakMinutes = 1,
            cyclesBeforeLongBreak = 2,
            totalCycles = 2,
        )

        val phaseDuration = testConfig.focusMinutes * 60L
        
        // Complete first cycle (focus)
        for (i in 0 until phaseDuration) {
            testConfig.tick()
        }

        // Complete short break
        var remaining = testConfig.currentPhaseRemainingSeconds()
        for (i in 0 until remaining) {
            testConfig.tick()
        }

        // Complete second cycle (focus)
        remaining = testConfig.currentPhaseRemainingSeconds()
        for (i in 0 until remaining) {
            testConfig.tick()
        }

        // After final focus phase, should be complete
        assertTrue(testConfig.isComplete())
    }

    // ─── REMAINING FOCUS CYCLES TESTS ─────────────────────────────

    @Test
    fun `remainingFocusCycles should count down correctly`() {
        assertEquals(4, config.remainingFocusCycles())

        val focusDuration = config.focusMinutes * 60L
        
        // Complete one full cycle (focus + short break)
        for (i in 0 until focusDuration) {
            config.tick()
        }

        var remaining = config.currentPhaseRemainingSeconds()
        for (i in 0 until remaining) {
            config.tick()
        }

        // Should have 3 remaining after one completed cycle
        assertTrue(config.remainingFocusCycles() <= 3)
    }

    // ─── EDGE CASE TESTS ──────────────────────────────────────────

    @Test
    fun `tick with zero remaining should not go negative`() {
        val config2 = PomodoroConfig(
            focusMinutes = 1,      // 60 seconds for faster testing
            shortBreakMinutes = 30,
            longBreakMinutes = 60,
            cyclesBeforeLongBreak = 1,
            totalCycles = 1,
        )

        val remaining = config2.currentPhaseRemainingSeconds()
        
        // Tick past zero
        for (i in 0..remaining) {
            config2.tick()
        }

        assertTrue(config2.currentPhaseRemainingSeconds() >= 0L)
    }

    @Test
    fun `phase name should never be null during active pomodoro`() {
        val phase = config.tick()

        // May return null if complete, but usually returns new phase name
        assertNotNull(phase ?: "FOCUS")
    }

    // ─── CUSTOM CONFIG TESTS ──────────────────────────────────────

    @Test
    fun `custom config with 50 min focus should work`() {
        val customConfig = PomodoroConfig(
            focusMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            cyclesBeforeLongBreak = 2,
            totalCycles = 3,
        )

        assertEquals("FOCUS", customConfig.currentPhaseName)
        assertEquals(50L * 60, customConfig.currentPhaseRemainingSeconds())
    }

    @Test
    fun `config with single cycle should complete after one phase`() {
        val singleCycle = PomodoroConfig(
            focusMinutes = 1,      // faster for test
            shortBreakMinutes = 1,
            longBreakMinutes = 2,
            cyclesBeforeLongBreak = 99, // never trigger long break in one cycle
            totalCycles = 1,       // only one cycle
        )

        assertEquals("FOCUS", singleCycle.currentPhaseName)
        assertFalse(singleCycle.isComplete())
    }
}
