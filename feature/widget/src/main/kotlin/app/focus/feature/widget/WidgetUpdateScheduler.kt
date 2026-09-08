package app.focus.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdateScheduler {
    private var lastUpdateAtMillis: Long = 0L

    suspend fun requestUpdate(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateAtMillis < MIN_UPDATE_INTERVAL_MS) return
        lastUpdateAtMillis = now
        FocusWidget().updateAll(context)
    }

    private const val MIN_UPDATE_INTERVAL_MS = 60_000L
}
