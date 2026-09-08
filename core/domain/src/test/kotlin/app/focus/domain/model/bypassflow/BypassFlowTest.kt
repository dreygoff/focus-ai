package app.focus.domain.model.bypassflow

import app.focus.domain.model.BreathingPhase
import app.focus.domain.model.BypassState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = true,
            delaySeconds = 5,
            breathingEnabled = false,
            reasonRequired = true,
        )
        flow = BypassFlow(config = config, packageName = "com.instagram.android", initialBypassesUsed = 0)
    }

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
        for (i in 0 until 4) {
            flow.tickDelay()
        }
        flow.tickDelay()
        flow.submitReason("Valid reason text")
        assertTrue(!flow.isInProgress())
    }

    @Test
    fun `tickDelay should decrease remaining time by 1 second`() {
        flow.start()
        val ticked = flow.tickDelay()
        assertTrue(ticked is BypassState.Delay)
        assertEquals(4, (ticked as BypassState.Delay).remainingSeconds)
    }

    @Test
    fun `tickDelay should complete when reaching 0`() {
        flow.start()
        for (i in 0 until 5) {
            flow.tickDelay()
        }
        assertTrue(flow.getState() is BypassState.Reason)
    }

    @Test
    fun `tickDelay should return null when not in Delay state`() {
        assertNull(flow.tickDelay())
    }

    @Test
    fun `submitReason should fail when text too short`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            reasonMinLength = 10,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        flow2.start()

        assertFalse(flow2.submitReason("short"))
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

        assertTrue(flow2.submitReason("Valid reason text"))
        assertTrue(flow2.getState() is BypassState.Granted)
    }

    @Test
    fun `inputPhraseCharacter should advance on correct character`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            phraseRequired = true,
            phraseText = "ABC",
            reasonMinLength = 5,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        flow2.start()
        flow2.submitReason("Valid reason")

        val state = flow2.inputPhraseCharacter('a')
        assertNotNull(state)
        assertTrue((state as BypassState.Phrase).characterIndex == 1)
        assertEquals("a", state.typedText)
    }

    @Test
    fun `inputPhraseCharacter should reset on wrong character`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = true,
            phraseRequired = true,
            phraseText = "ABC",
            reasonMinLength = 5,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", 0)
        flow2.start()
        flow2.submitReason("Valid reason")

        val wrongCharState = flow2.inputPhraseCharacter('Z')
        assertNotNull(wrongCharState)
        assertTrue(wrongCharState is BypassState.Idle)
    }

    @Test
    fun `checkBypassLimit should return true when under limit`() {
        val config = BypassFlowConfig.Default.copy(bypassLimitPerSession = 3)
        val flow2 = BypassFlow(config, "com.instagram.android", initialBypassesUsed = 2)
        assertTrue(flow2.checkBypassLimit())
    }

    @Test
    fun `resetToIdle should return to Idle state`() {
        flow.start()
        val resetState = flow.resetToIdle()
        assertTrue(resetState is BypassState.Idle)
        assertTrue(flow.getState() is BypassState.Idle)
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

        val endAt = flow2.getAccessWindowEndAt()
        assertNotNull(endAt)
        assertTrue(endAt!! > System.currentTimeMillis())
    }

    @Test
    fun `bypass denied when limit reached`() {
        val config = BypassFlowConfig.Default.copy(
            delayEnabled = false,
            reasonRequired = false,
            bypassLimitPerSession = 0,
        )
        val flow2 = BypassFlow(config, "com.instagram.android", initialBypassesUsed = 0)
        flow2.start()
        assertTrue(flow2.getState() is BypassState.Denied)
    }
}
