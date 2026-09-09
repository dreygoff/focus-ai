package app.focus.system

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Per TR-01: UsageStatsPollingDetector fallback detector.
 * Polls UsageStatsManager every 500-1000ms for foreground app.
 */
class DefaultUsageStatsPollingDetector(
    private val context: Context
) : UsageStatsPollingDetector {

    companion object {
        private const val TAG = "UsageStatsPollingDetector"
    }

    private val _events = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 64)
    override val events: Flow<FocusEvent> = _events.asSharedFlow()

    private var running = false
    private var lastPkg: String? = null

    // Adaptive interval: 300ms initially, then 700-1000ms
    private var pollIntervalMs: Long = 1000

    @SuppressLint("MissingPermission")
    override fun start() {
        if (running) return
        running = true

        android.os.Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (!running) return

                try {
                    val now = System.currentTimeMillis()
                    val statsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return

                    // Query events from last 2000ms
                    val eventsList = statsManager.queryEvents(now - 2000, now)

                    var foundPkg: String? = null
                    val usageEvent = UsageEvents.Event()
                    while (eventsList.hasNextEvent()) {
                        eventsList.getNextEvent(usageEvent)
                        val isForegroundEvent = usageEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                            usageEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                        if (isForegroundEvent) {
                            val pkg = usageEvent.packageName
                            if (pkg.isNotEmpty() && !isFocusProcess(pkg)) {
                                foundPkg = pkg
                            }
                        }
                    }

                    if (foundPkg != null && foundPkg != lastPkg) {
                        lastPkg = foundPkg
                        _events.tryEmit(FocusEvent(
                            packageName = foundPkg!!,
                            appName = "",
                            timestampMillis = now
                        ))
                        // Rapid polling after detection
                        pollIntervalMs = 300
                    } else if (foundPkg == null) {
                        // Slow down polling
                        pollIntervalMs = kotlin.math.min(pollIntervalMs + 200, 1000)
                    }

                } catch (_: Exception) {}

                if (running) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, pollIntervalMs)
                }
            }
        })
    }

    override fun stop(): Boolean {
        running = false
        return true
    }

    override fun onDestroy() {
        running = false
    }

    private fun isFocusProcess(pkg: String): Boolean =
        pkg == context.packageName || pkg.contains("com.android.systemui")
}
