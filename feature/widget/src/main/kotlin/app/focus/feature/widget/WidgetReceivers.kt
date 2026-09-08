package app.focus.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.focus.common.WidgetActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != WidgetActions.REFRESH) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                WidgetUpdateScheduler.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entryPoint = widgetEntryPoint(context)
                when (intent?.action) {
                    WidgetActions.START_SESSION -> entryPoint.quickStartLastProfileUseCase().execute()
                    WidgetActions.STOP_SESSION -> entryPoint.quickStopSessionUseCase().execute()
                }
                WidgetUpdateScheduler.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
