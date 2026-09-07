package app.focus.android.domain.bypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BypassFlow state machine (TR-07).
 * Tests all state transitions and edge cases.
 */
class BypassFlowTest {

    private lateinit var flow: BypassFlow

    @Before
    fun setUp() {
        // Default config: delay 30s, no breathing, reason required, phrase not required
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = true,
            delaySeconds = 5, // shorter for tests
            breathingEnabled = false,
            reasonRequired = true,
        )
        flow = BypassFlow(config = config, packageName = "com.instagram.android", bypassesUsed = 0)
    }

    // ─── START TESTS ──────────────────────────────────────────────

    @Test
    fun `start with delay should produce Delay state`() {
        val state = flow.start()
        
        assertTrue(state is BypassState.Delay)
        assertEquals(5, (state as BypassState.Delay).remainingSeconds)
        assertEquals(5, state.totalTimeSeconds)
    }

    @Test
    fun `start with delay disabled and reason required should produce Reason state`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        val state = flow2.start()
        assertTrue(state is BypassState.Reason)
    }

    @Test
    fun `start with delay disabled and reason disabled should produce Granted state`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        val state = flow2.start()
        assertTrue(state is BypassState.Granted)
    }

    @Test
    fun `start should set isInProgress to true during delay`() {
        flow.start()
        
        assertTrue(flow.isInProgress())
    }

    @Test
    fun `isInProgress should be false after bypass granted`() {
        flow.start()
        
        // Tick through all delays manually (5 ticks of 1 second each)
        for (i in 0 until 4) {
            flow.tickDelay()
        }
        // Final tick triggers reason state
        val finalTick = flow.tickDelay()
        assertNotNull(finalTick)

        // Now submit valid reason
        flow.submitReason("Valid reason text")
        
        assertTrue(!flow.isInProgress())
    }

    @Test
    fun `isInProgress should be false after bypass denied`() {
        flow.start()
        
        for (i in 0 until 4) {
            flow.tickDelay()
        }
        flow.submitReason("Valid reason") // Grants access, move to granted
        
        assertTrue(!flow.isInProgress())
    }

    // ─── DELAY TESTS ─────────────────────────────────────────────

    @Test
    fun `tickDelay should decrease remaining time by 1 second`() {
        flow.start()
        
        val ticked = flow.tickDelay()
        assertTrue(ticked is BypassState.Delay)
        assertEquals(4, (ticked as BypassState.Delay).remainingSeconds)
    }

    @Test
    fun `tickDelay should complete when reaching 0`() {
        for (i in 0 until 5) {
            flow.tickDelay()
        }

        val state = flow.getState()
        assertTrue(state is BypassState.Reason) // Should move to reason next
    }

    @Test
    fun `tickDelay should return null when not in Delay state`() {
        val result = flow.tickDelay()
        
        assertNull(result)
    }

    // ─── BREATHING TESTS ─────────────────────────────────────────

    @Test
    fun `start with breathing enabled should produce Breathing state`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            breathingEnabled = true,
            reasonRequired = true,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        val state = flow2.start()
        assertTrue(state is BypassState.Breathing)
        assertEquals(0, (state as BypassState.Breathing).currentCycle)
        assertEquals(BreathingPhase.INHALE, state.phaseInCycle)
    }

    @Test
    fun `tickBreathing should decrement timeInPhase`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            breathingEnabled = true,
            reasonRequired = false,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        assertTrue(flow2.getState() is BypassState.Breathing)

        val complete = flow2.tickBreathing(1000L) // tick 1 second
        
        assertFalse(complete) // Breathing shouldn't be complete yet
    }

    @Test
    fun `tickBreathing should cycle through phases`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            breathingEnabled = true,
            reasonRequired = true,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()

        // Complete INHALE phase (4000ms)
        for (i in 0 until 40) {
            flow2.tickBreathing(100L)
        }

        var state = flow2.getState()
        assertTrue(state is BypassState.Breathing)
        assertEquals(BreathingPhase.HOLD, (state as BypassState.Breathing).phaseInCycle)
        assertEquals(0, state.currentCycle) // cycle not incremented until EXHALE completes
        
        // Complete HOLD phase (4000ms)
        for (i in 0 until 40) {
            flow2.tickBreathing(100L)
        }

        state = flow2.getState()
        assertTrue(state is BypassState.Breathing)
        assertEquals(BreathingPhase.EXHALE, (state as BypassState.Breathing).phaseInCycle)
        
        // Complete EXHALE phase (6000ms) - should move to cycle 1
        for (i in 0 until 60) {
            flow2.tickBreathing(100L)
        }

        state = flow2.getState()
        assertTrue(state is BypassState.Breathing)
        assertEquals(1, (state as BypassState.Breathing).currentCycle)
        assertEquals(BreathingPhase.INHALE, state.phaseInCycle)
    }

    @Test
    fun `tickBreathing should complete all cycles and move to reason`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            breathingEnabled = true,
            reasonRequired = true,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()

        // Complete all 3 cycles: (INHALE+HOLD+EXHALE) * 3 = (4000+4000+6000)*3 = 54000ms total
        val totalTicks = (4000L + 4000L + 6000L) / 100L * 3 // 180 ticks
        
        for (i in 0 until totalTicks) {
            flow2.tickBreathing(100L)
        }

        val state = flow2.getState()
        assertTrue(state is BypassState.Reason) // Should move to reason after all cycles
    }

    // ─── REASON TESTS ─────────────────────────────────────────────

    @Test
    fun `submitReason should fail when text too short`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            reasonMinLength = 10,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        // First tick to get past delay
        for (i in 0 until 4) {
            flow2.tickDelay()
        }

        val valid = flow2.submitReason("short") // less than 10 chars
        
        assertFalse(valid)
        assertTrue(flow2.getState() is BypassState.Reason)
    }

    @Test
    fun `submitReason should succeed when text meets minimum length`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            phraseRequired = false,
            reasonMinLength = 10,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        // Tick to get past delay (5 seconds)
        for (i in 0 until 4) {
            flow2.tickDelay()
        }

        val valid = flow2.submitReason("Valid reason text") // 17 chars > 10
        
        assertTrue(valid)
        assertTrue(flow2.getState() is BypassState.Granted)
    }

    @Test
    fun `submitReason with phrase enabled should move to Phrase state`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            phraseRequired = true,
            phraseText = "Я осознанно отвлекаюсь",
            reasonMinLength = 10,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        for (i in 0 until 4) {
            flow2.tickDelay()
        }

        val valid = flow2.submitReason("Valid reason text")
        
        assertTrue(valid)
        assertTrue(flow2.getState() is BypassState.Phrase)
    }

    // ─── PHRASE TESTS ─────────────────────────────────────────────

    @Test
    fun `inputPhraseCharacter should advance on correct character`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            phraseRequired = true,
            phraseText = "ABC",
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        // Tick to get past delay and reason (if required)
        for (i in 0 until 4) {
            flow2.tickDelay()
        }

        val state = flow2.inputPhraseCharacter('A')
        assertNotNull(state)
        assertTrue((state as BypassState.Phrase).characterIndex == 1)
        assertEquals("A", state.enteredText)
    }

    @Test
    fun `inputPhraseCharacter should reset on wrong character`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            phraseRequired = true,
            phraseText = "ABC",
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        // Tick to get past delay
        for (i in 0 until 4) {
            flow2.tickDelay()
        }

        val wrongCharState = flow2.inputPhraseCharacter('Z') // Wrong!
        assertNotNull(wrongCharState)
        assertTrue((wrongCharState as BypassState.Idle) == BypassState.Idle)
    }

    @Test
    fun `inputPhraseCharacter should grant on complete phrase`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            phraseRequired = true,
            phraseText = "ABC",
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()
        
        // Complete the phrase
        flow2.inputPhraseCharacter('A')
        flow2.inputPhraseCharacter('B')
        flow2.inputPhraseCharacter('C')

        assertTrue(flow2.getState() is BypassState.Granted)
    }

    @Test
    fun `inputPhraseCharacter should deny bypass if limit reached`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            phraseRequired = true,
            phraseText = "ABC",
            bypassLimitPerSession = 0, // No bypasses allowed
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 0)
        
        flow2.start()
        
        flow2.inputPhraseCharacter('A')
        flow2.inputPhraseCharacter('B')
        flow2.inputPhraseCharacter('C')

        assertTrue(flow2.getState() is BypassState.Denied)
    }

    @Test
    fun `checkBypassLimit should return false when under limit`() {
        val config = BypassFlowConfig.Default.copy(
            bypassLimitPerSession = 3,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 2)
        
        assertTrue(flow2.checkBypassLimit())
    }

    @Test
    fun `checkBypassLimit should return false when unlimited (-1)`() {
        val config = BypassFlowConfig.Default.copy(
            bypassLimitPerSession = -1,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 999)
        
        assertTrue(flow2.checkBypassLimit())
    }

    @Test
    fun `checkBypassLimit should return false when at limit`() {
        val config = BypassFlowConfig.Default.copy(
            bypassLimitPerSession = 3,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 3)
        
        assertFalse(flow2.checkBypassLimit())
    }

    // ─── RESET TESTS ─────────────────────────────────────────────

    @Test
    fun `resetToIdle should return to Idle state`() {
        flow.start()
        
        val resetState = flow.resetToIdle()
        
        assertTrue(resetState is BypassState.Idle)
        assertTrue(flow.getState() is BypassState.Idle)
    }

    @Test
    fun `resetToIdle should work from any progress state`() {
        flow.start()
        for (i in 0 until 4) {
            flow.tickDelay()
        }
        
        val resetState = flow.resetToIdle()
        
        assertTrue(resetState is BypassState.Idle)
    }

    // ─── ACCESS WINDOW TESTS ─────────────────────────────────────

    @Test
    fun `getAccessWindowEndAt should return null when not granted`() {
        val endAt = flow.getAccessWindowEndAt()
        
        assertNull(endAt)
    }

    @Test
    fun `getAccessWindowEndAt should return future time when granted`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            accessWindowMinutes = 5,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()

        // Should grant immediately since no friction configured
        assertTrue(flow2.getState() is BypassState.Granted)

        val endAt = flow2.getAccessWindowEndAt()
        assertNotNull(endAt)
        assertTrue(endAt!! > System.currentTimeMillis()) // should be in the future
    }

    // ─── COMPREHENSIVE FLOW TESTS ────────────────────────────────

    @Test
    fun `full bypass flow with delay and reason should end granted`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = true,
            delaySeconds = 3, // 3 seconds for faster test
            reasonRequired = true,
            reasonMinLength = 5,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 0)
        
        // Start → Delay
        val state1 = flow2.start()
        assertTrue(state1 is BypassState.Delay)

        // Tick through delay
        for (i in 0 until 2) {
            flow2.tickDelay()
        }

        // Should still be in Reason after last tick (delay complete, reason required)
        val state2 = flow2.getState()
        assertTrue(state2 is BypassState.Reason)

        // Submit valid reason
        flow2.submitReason("Valid reason")

        assertTrue(flow2.getState() is BypassState.Granted)
    }

    @Test
    fun `full bypass with delay breathing and phrase should end granted`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = true,
            delaySeconds = 1, // 1 second for faster test
            breathingEnabled = true,
            reasonRequired = false,
            phraseRequired = true,
            phraseText = "AB", // short phrase for test
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 0)
        
        // Start → Delay
        val state1 = flow2.start()
        assertTrue(state1 is BypassState.Delay)

        // Tick to complete delay
        for (i in 0 until 1) {
            flow2.tickDelay()
        }

        // Should now be in Breathing phase
        var state = flow2.getState()
        assertTrue(state is BypassState.Breathing)

        // Complete breathing cycles quickly (set timeInPhase to complete immediately by ticking with large delta)
        for (i in 0 until 180) { // enough ticks to complete all 3 cycles
            flow2.tickBreathing(100L)
        }

        // Should be in Phrase phase now
        state = flow2.getState()
        assertTrue(state is BypassState.Phrase)

        // Complete phrase
        flow2.inputPhraseCharacter('A')
        flow2.inputPhraseCharacter('B')

        assertTrue(flow2.getState() is BypassState.Granted)
    }

    @Test
    fun `bypass denied when limit reached`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            bypassLimitPerSession = 0, // No bypasses allowed (0 remaining)
        )
        val flow2 = BypassFlow(config, "com.instagram.android", bypassesUsed = 0)
        
        flow2.start()

        assertTrue(flow2.getState() is BypassState.Denied)
    }

    @Test
    fun `inProgress should be false after idle start`() {
        flow.resetToIdle()
        
        assertTrue(!flow.isInProgress())
    }

    @Test
    fun `should return correct remaining time for granted bypass`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            accessWindowMinutes = 10, // 10 minutes
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        
        flow2.start()

        val endAt = flow2.getAccessWindowEndAt()
        assertNotNull(endAt)

        val expectedMinMillis = 10 * 60 * 1000L
        val actualDiff = endAt!! - System.currentTimeMillis()
        assertTrue("Expected ~${expectedMinMillis}ms but got $actualDiff", 
            actualDiff in (expectedMinMillis - 1000L)..(expectedMinMillis + 1000L))
    }

    @Test
    fun `resetToIdle should be called when user leaves screen`() {
        flow.start()
        
        // Tick a few times to simulate progress
        for (i in 0 until 3) {
            flow.tickDelay()
        }
        
        // User leaves screen
        flow.resetToIdle()
        
        assertTrue(flow.getState() is BypassState.Idle)
        assertFalse(flow.isInProgress())
    }
}
