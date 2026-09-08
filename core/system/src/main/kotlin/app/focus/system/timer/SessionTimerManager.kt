package app.focus.system.timer

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.focus.domain.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per TR-04: SessionTimerManager tracks elapsed/remaining time for focus sessions.
 * Updates timer every 100ms and fires events at status boundaries.
 */
class SessionTimerManager(
    private val context: Context,
    initialSessionId: String?,
    initialPlannedEndAtMillis: Long?
) {

    companion object {
        private const val TAG = "SessionTimerManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _status = MutableStateFlow<SessionStatus>(SessionStatus.Idle)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var handler: Handler? = null
    private var running = false
    private var startedAtMillis: Long = 0L
    private var plannedEndAtMillis: Long = 0L

    val sessionId: String? = initialSessionId

    val totalDurationMs: Long get() = plannedEndAtMillis - startedAtMillis

    init {
        if (initialPlannedEndAtMillis != null) {
            plannedEndAtMillis = initialPlannedEndAtMillis
        }
        if (initialSessionId != null) {
            _status.value = SessionStatus.Running
            running = true
            startedAtMillis = System.currentTimeMillis()
            startTimer()
        }
    }

    fun start(plannedEndAtMillis: Long) {
        this.plannedEndAtMillis = plannedEndAtMillis
        if (!running) {
            running = true
            startedAtMillis = System.currentTimeMillis()
            _status.value = SessionStatus.Running
            startTimer()
        }
    }

    fun pause() {
        running = false
        if (_status.value is SessionStatus.Running) {
            _status.value = SessionStatus.Paused()
            stopTimer()
        }
    }

    fun resume(plannedEndAtMillis: Long) {
        if (_status.value !is SessionStatus.Paused) return

        this.plannedEndAtMillis = plannedEndAtMillis
        startedAtMillis = System.currentTimeMillis() - elapsedMs.value
        running = true
        _status.value = SessionStatus.Running
        startTimer()
    }

    fun forceComplete(status: SessionStatus) {
        stopTimer()
        running = false
        _status.value = status
    }

    private fun startTimer() {
        handler = Handler(Looper.getMainLooper())
        val tickRunnable = object : Runnable {
            override fun run() {
                if (!running) return

                val now = System.currentTimeMillis()
                val elapsed = now - startedAtMillis
                _elapsedMs.value = elapsed

                val remaining = plannedEndAtMillis - now
                _remainingMs.value = maxOf(0L, remaining)

                when {
                    remaining <= 0 -> {
                        _status.value = SessionStatus.Completed
                        running = false
                    }
                    elapsed < 5_000 && _status.value !is SessionStatus.Running -> {
                        _status.value = SessionStatus.Starting
                    }
                }

                handler?.postDelayed(this, 100)
            }
        }
        handler?.post(tickRunnable)
    }

    private fun stopTimer() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    fun destroy() {
        stopTimer()
        scope.cancel()
    }

    val isActive: Boolean get() = running

    val elapsedSeconds: Long get() = elapsedMs.value / 1000

    val remainingSeconds: Long get() = remainingMs.value / 1000
}
