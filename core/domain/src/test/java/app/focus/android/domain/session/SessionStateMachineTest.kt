package app.focus.android.domain.session

import app.focus.android.domain.clock.TestClock
import app.focus.domain.model.LockMode
import app.focus.domain.model.PomodoroConfig
import app.focus.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionStateMachine covering all state transitions and invariants (§10 spec).
 */
class SessionStateMachineTest {

    private lateinit var machine: SessionStateMachine
    private lateinit var clock: TestClock

    @Before
    fun setUp() {
        machine = SessionStateMachine()
        clock = TestClock(initialTime = 1_700_000_000_000L) // Fixed base time
    }

    // ─── START TESTS ──────────────────────────────────────────────

    @Test
    fun `start should transition from Idle to Running`() {
        assertTrue(machine.getState() is SessionState.Idle)
        
        val session = machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            plannedDurationMinutes = 25L,
        )

        assertTrue(machine.getState() is SessionState.Running)
        assertEquals(SessionStatus.Running, session.status)
        assertNotNull(session.id)
        assertNotNull(session.startedAt)
        assertNull(session.actualEndAt)
    }

    @Test
    fun `start with hard lock must have finite duration`() {
        val result = kotlin.runCatching {
            machine.start(
                profileId = "profile-1",
                profileName = "Hard Focus",
                lockMode = LockMode.Hard,
                targetPackages = emptyList(),
                plannedDurationMinutes = null, // infinite duration not allowed for hard
            )
        }

        assertFalse(result.isSuccess)
    }

    @Test(expected = IllegalStateException::class)
    fun `start should fail if already running`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        // Should throw - cannot start while running
        machine.start(
            profileId = "profile-2",
            profileName = "Focus 2",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 15L,
        )
    }

    // ─── PAUSE TESTS ──────────────────────────────────────────────

    @Test
    fun `pause should transition Running to Paused for soft lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Soft Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        val result = machine.pause(maxPauses = 3, maxPauseMinutes = 10)
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Paused)
    }

    @Test
    fun `pause should fail for hard lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        val result = machine.pause(maxPauses = 3, maxPauseMinutes = 10)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `pause should fail when pause limit is exceeded`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Soft Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        // First pause - OK
        val result1 = machine.pause(maxPauses = 2, maxPauseMinutes = 10)
        assertTrue(result1.isSuccess)

        // Second pause - OK (limit is 2)
        machine.resume()
        val result2 = machine.pause(maxPauses = 2, maxPauseMinutes = 10)
        assertTrue(result2.isSuccess)

        // Third pause - should fail (limit exceeded)
        machine.resume()
        val result3 = machine.pause(maxPauses = 2, maxPauseMinutes = 10)
        assertFalse(result3.isSuccess)
    }

    @Test
    fun `pause should fail when not in Running state`() {
        val result = machine.pause(maxPauses = 3, maxPauseMinutes = 10)
        assertFalse(result.isSuccess)
    }

    // ─── RESUME TESTS ─────────────────────────────────────────────

    @Test
    fun `resume should transition Paused to Running`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Soft Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )
        machine.pause(maxPauses = 3, maxPauseMinutes = 10)

        val result = machine.resume()
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Running)
    }

    @Test
    fun `resume should fail when not in Paused state`() {
        val result = machine.resume()
        assertFalse(result.isSuccess)
    }

    // ─── END SESSION TESTS ────────────────────────────────────────

    @Test
    fun `endSession should transition Running to Completed`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        val session = machine.endSession(actualEndAt = clock.currentTime + 1_500_000L) // 25 min
        
        assertTrue(machine.getState() is SessionState.Completed)
        assertEquals(SessionStatus.Completed, session.status)
        assertNotNull(session.actualEndAt)
    }

    @Test
    fun `endSession should fail when not in Running state`() {
        val result = runCatching { machine.endSession(clock.currentTime) }
        assertFalse(result.isSuccess)
    }

    // ─── CANCEL SESSION TESTS ─────────────────────────────────────

    @Test
    fun `cancelSession should transition Running to Cancelled for soft lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Soft Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        val result = machine.cancelSession(clock.currentTime + 600_000L) // 10 min in
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Cancelled)
    }

    @Test
    fun `cancelSession should fail for hard lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        val result = machine.cancelSession(clock.currentTime + 3_600_000L)
        assertFalse(result.isSuccess)
    }

    // ─── EMERGENCY EXIT TESTS ─────────────────────────────────────

    @Test
    fun `requestEmergencyExit should transition Running to ExitPending for hard lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        val result = machine.requestEmergencyExit(delayMinutes = 10)
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.ExitPending)
    }

    @Test
    fun `requestEmergencyExit should fail for soft lock`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Soft Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        val result = machine.requestEmergencyExit(delayMinutes = 10)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `confirmEmergencyExit should complete when pending time has elapsed`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        machine.requestEmergencyExit(delayMinutes = 10)
        
        // Fast forward 11 minutes
        clock.advance(11 * 60_000L)

        val result = machine.confirmEmergencyExit()
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Cancelled)
    }

    @Test
    fun `confirmEmergencyExit should fail when pending time has not elapsed`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        machine.requestEmergencyExit(delayMinutes = 10)
        
        // Only advance 5 minutes
        clock.advance(5 * 60_000L)

        val result = machine.confirmEmergencyExit()
        assertFalse(result.isSuccess)
    }

    @Test
    fun `recallEmergencyExit should transition ExitPending back to Running`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        machine.requestEmergencyExit(delayMinutes = 10)
        
        // Recall within pending period
        val result = machine.recallEmergencyExit()
        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Running)
    }

    @Test
    fun `recallEmergencyExit should fail after pending time has elapsed`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        machine.requestEmergencyExit(delayMinutes = 10)
        
        // Advance past pending time
        clock.advance(11 * 60_000L)

        val result = machine.recallEmergencyExit()
        assertFalse(result.isSuccess)
    }

    // ─── EXPIRE SESSION TESTS ─────────────────────────────────────

    @Test
    fun `expireSession should transition Running to Expired`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        val session = machine.expireSession(plannedEndAt = clock.currentTime - 60_000L) // expired 1 min ago
        
        assertTrue(machine.getState() is SessionState.Expired)
        assertEquals(SessionStatus.Expired, session.status)
    }

    @Test
    fun `expireSession should fail when not in Running state`() {
        val result = runCatching { machine.expireSession(clock.currentTime) }
        assertFalse(result.isSuccess)
    }

    // ─── RESTORE FROM SNAPSHOT TESTS ──────────────────────────────

    @Test
    fun `restoreFromSnapshot should restore active session`() {
        val result = machine.restoreFromSnapshot(
            sessionId = "restored-session-id",
            lockMode = LockMode.Hard,
            plannedEndAt = clock.currentTime + 3_600_000L, // 1 hour from now
            targetPackages = listOf("com.instagram.android"),
        )

        assertTrue(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Running)
    }

    @Test
    fun `restoreFromSnapshot should fail if session has already expired`() {
        val result = machine.restoreFromSnapshot(
            sessionId = "expired-session-id",
            lockMode = LockMode.Hard,
            plannedEndAt = clock.currentTime - 60_000L, // expired 1 minute ago
            targetPackages = emptyList(),
        )

        assertFalse(result.isSuccess)
        assertTrue(machine.getState() is SessionState.Expired)
    }

    // ─── POMODORO TESTS ───────────────────────────────────────────

    @Test
    fun `advancePomodoroPhase should update phase`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Pomodoro Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )

        val result = machine.advancePomodoroPhase("BREAK")
        assertTrue(result.isSuccess)
        
        val state = machine.getState() as SessionState.Running
        assertEquals("BREAK", state.pomodoroPhase)
    }

    // ─── BYPASS INCREMENT TESTS ───────────────────────────────────

    @Test
    fun `incrementBypasses should return false when under limit`() {
        assertFalse(machine.incrementBypasses(currentCount = 0, limit = 3))
        assertFalse(machine.incrementBypasses(currentCount = 1, limit = 3))
        assertFalse(machine.incrementBypasses(currentCount = 2, limit = 3))
    }

    @Test
    fun `incrementBypasses should return true when limit is reached`() {
        assertTrue(machine.incrementBypasses(currentCount = 3, limit = 3))
    }

    @Test
    fun `incrementBypasses with unlimited limit (-1) never denies`() {
        assertFalse(machine.incrementBypasses(currentCount = 0, limit = -1))
        assertFalse(machine.incrementBypasses(currentCount = 999, limit = -1))
    }

    // ─── HELPER METHODS TESTS ─────────────────────────────────────

    @Test
    fun `isActive should return true for Running and Paused`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )
        assertTrue(machine.isActive())

        machine.pause()
        assertTrue(machine.isActive())
    }

    @Test
    fun `isActive should return false for terminal states`() {
        assertFalse(machine.isActive()) // Idle
        
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )
        machine.endSession(clock.currentTime)
        assertFalse(machine.isActive())

        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )
        machine.cancelSession(clock.currentTime)
        assertFalse(machine.isActive())
    }

    @Test
    fun `isEmergencyExitElapsed should reflect pending time`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Hard Focus",
            lockMode = LockMode.Hard,
            targetPackages = emptyList(),
            plannedDurationMinutes = 60L,
        )

        machine.requestEmergencyExit(delayMinutes = 10)
        assertFalse(machine.isEmergencyExitElapsed())

        clock.advance(5 * 60_000L) // 5 minutes
        assertFalse(machine.isEmergencyExitElapsed())

        clock.advance(6 * 60_000L) // total 11 minutes > 10 min delay
        assertTrue(machine.isEmergencyExitElapsed())
    }

    // ─── INVARIANT TESTS ──────────────────────────────────────────

    @Test(expected = IllegalStateException::class)
    fun `invariant: cannot start while running`() {
        machine.start(
            profileId = "profile-1",
            profileName = "Focus",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 25L,
        )
        
        // Cannot start another session while first is running
        machine.start(
            profileId = "profile-2",
            profileName = "Focus 2",
            lockMode = LockMode.Soft,
            targetPackages = emptyList(),
            plannedDurationMinutes = 15L,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `invariant: cannot pause while not running`() {
        machine.pause() // Idle -> can't pause
    }

    @Test(expected = IllegalStateException::class)
    fun `invariant: cannot resume while not paused`() {
        machine.resume() // Idle -> can't resume
    }
}
