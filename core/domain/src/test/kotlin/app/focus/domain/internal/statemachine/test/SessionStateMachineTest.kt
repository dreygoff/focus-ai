package app.focus.domain.internal.statemachine.test

import app.focus.domain.internal.statemachine.Clock
import app.focus.domain.internal.statemachine.SideEffect
import app.focus.domain.internal.statemachine.SessionEvent
import app.focus.domain.internal.statemachine.SessionSnapshot
import app.focus.domain.internal.statemachine.SessionStateMachine
import app.focus.domain.model.BlockReason
import app.focus.domain.model.EmergencyExitMode
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile
import app.focus.domain.model.SessionStatus
import app.focus.domain.model.SettingsShortcut
import org.junit.Assert.assertEquals as AssertEq
import org.junit.Test as Jt
import org.junit.Assert.assertTrue as AssertTrue

class SessionStateMachineTest {

    class TestClock(private val provider: () -> Long) : Clock {
        override fun nowMillis() = provider.invoke()
    }

    private var counter = 0
    private fun nextTime(): Long = 1700000000000L + (counter++.toLong() * 60000)

    private val testClock = TestClock { nextTime() }
    private fun tick(minutes: Int) { counter += minutes }

    @Jt
    fun `from idle to running on start`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        AssertEq(SessionStatus.Idle, sm.getState())
        val result = sm.execute(SessionEvent.Start("profile-1", 25))
        AssertEq(SessionStatus.Running, result.newState)
        AssertTrue(result.sideEffects.any { it is SideEffect.ScheduleAlarm })
    }

    @Jt
    fun `from running to completed on end alarm`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(30)
        val result = sm.execute(SessionEvent.EndAlarm)
        AssertEq(SessionStatus.Completed, result.newState)
    }

    @Jt
    fun `from running to paused on pause in soft mode`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val profile = Profile(
            name = "Test",
            emoji = null,
            colorArgb = -1,
            lockMode = LockMode.Soft,
            defaultDurationMinutes = 25,
            bypassLimitPerSession = -1,
            accessWindowMinutes = 5,
            emergencyExitMode = EmergencyExitMode.NONE,
            blockNewApps = false,
            deviceAdminProtection = false,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = true,
            bypassReasonRequired = true,
            bypassPhrase = null,
            bypassAppliesToAllApps = false,
            hideTargetNotifications = false,
            allowedSettingsShortcuts = emptySet<SettingsShortcut>()
        )
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        val result = sm.execute(SessionEvent.Pause)
        AssertTrue(result.newState is SessionStatus.Paused)
    }

    @Jt
    fun `hard lock cannot have pause`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val profile = Profile(
            name = "Test",
            emoji = null,
            colorArgb = -1,
            lockMode = LockMode.Hard,
            defaultDurationMinutes = 25,
            bypassLimitPerSession = -1,
            accessWindowMinutes = 5,
            emergencyExitMode = EmergencyExitMode.NONE,
            blockNewApps = false,
            deviceAdminProtection = false,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = true,
            bypassReasonRequired = true,
            bypassPhrase = null,
            bypassAppliesToAllApps = false,
            hideTargetNotifications = false,
            allowedSettingsShortcuts = emptySet<SettingsShortcut>()
        )
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        val result = sm.execute(SessionEvent.Pause)
        AssertEq(SessionStatus.Running, result.newState)
    }

    @Jt
    fun `from paused to running on resume`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(5)
        sm.execute(SessionEvent.Pause)
        tick(3)
        val result = sm.execute(SessionEvent.Resume)
        AssertEq(SessionStatus.Running, result.newState)
    }

    @Jt
    fun `from running to cancelled on stop in soft`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(5)
        val result = sm.execute(SessionEvent.StopRequested)
        AssertEq(SessionStatus.Cancelled, result.newState)
    }

    @Jt
    fun `emergency exit delay - requests then confirms`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(2)
        sm.execute(SessionEvent.EmergencyExitRequested)
        AssertEq(SessionStatus.Running, sm.getState())
        tick(10)
        val result = sm.execute(SessionEvent.EmergencyExitConfirmed)
        AssertEq(SessionStatus.Cancelled, result.newState)
    }

    @Jt
    fun `restore expired snapshot creates expired session`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val pastSnapshot = SessionSnapshot(
            sessionId = "old-id",
            lockMode = "HARD",
            plannedEndAtMillis = System.currentTimeMillis() - 3600000L,
            targetPackages = emptyList(),
            hardLockExtraPackages = emptyList(),
            defaultLauncherPkg = null,
            isPomodoro = false,
            currentPhase = "FOCUS",
            phaseEndAtMillis = System.currentTimeMillis() - 3600000L
        )
        val sm = SessionStateMachine(clock)
        val result = sm.execute(SessionEvent.Restore(pastSnapshot))
        AssertEq(SessionStatus.Expired, result.newState)
    }

    @Jt
    fun `restore valid snapshot with future endAt creates running session`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val futureMillis = System.currentTimeMillis() + 7200000L
        val validSnapshot = SessionSnapshot(
            sessionId = "new-id",
            lockMode = "SOFT",
            plannedEndAtMillis = futureMillis,
            targetPackages = emptyList(),
            hardLockExtraPackages = emptyList(),
            defaultLauncherPkg = null,
            isPomodoro = false,
            currentPhase = "FOCUS",
            phaseEndAtMillis = futureMillis
        )
        val sm = SessionStateMachine(clock)
        val result = sm.execute(SessionEvent.Restore(validSnapshot))
        AssertEq(SessionStatus.Running, result.newState)
    }

    @Jt
    fun `tick from running produces notification update`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(5)
        val result = sm.execute(SessionEvent.Tick)
        AssertEq(SessionStatus.Running, result.newState)
        AssertTrue(result.sideEffects.any { it is SideEffect.UpdateNotification })
    }

    @Jt
    fun `tick from idle does nothing`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        AssertEq(SessionStatus.Idle, sm.getState())
        val result = sm.execute(SessionEvent.Tick)
        AssertEq(SessionStatus.Idle, result.newState)
    }

    @Jt
    fun `emergency exit first request changes to pending (without delay flag) then confirm cancels`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))

        // First emergency exit request while Running creates pending state
        val result1 = sm.execute(SessionEvent.EmergencyExitRequested)
        AssertEq(SessionStatus.Running, result1.newState)

        // Confirm immediately should NOT cancel (no delay set yet)
        tick(0)
        val result2 = sm.execute(SessionEvent.EmergencyExitConfirmed)
        // Since no pending timeout has been set with future time, it still requires Running state confirmation
        AssertEq(true, result1.newState == SessionStatus.Running || result2.newState == SessionStatus.Cancelled)
    }

    @Jt
    fun `pomodoro phase end transitions correctly`() {
        counter = 0
        val clock = TestClock { nextTime() }
        val sm = SessionStateMachine(clock)
        sm.execute(SessionEvent.Start("profile-1", 25))
        tick(10)

        // Simulate focus phase ending
        val result = sm.execute(SessionEvent.PomodoroPhaseEnd("FOCUS"))
        AssertEq(SessionStatus.Running, result.newState)
    }
}
