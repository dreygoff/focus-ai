package app.focus.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.focus.domain.usecase.WidgetMode
import app.focus.domain.usecase.WidgetSessionState

private const val WIDGET_BACKGROUND_COLOR = 0xFF2F6F6D
private const val WIDGET_SUBTITLE_COLOR = 0xFFE0F2F1

private val WidgetBackgroundColor = ColorProvider(Color(WIDGET_BACKGROUND_COLOR))
private val WidgetTextColor = ColorProvider(Color.White)
private val WidgetSubtitleColor = ColorProvider(Color(WIDGET_SUBTITLE_COLOR))

class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = runCatching {
            widgetEntryPoint(context).getWidgetSessionStateUseCase().execute()
        }.getOrDefault(
            WidgetSessionState(WidgetMode.IDLE, null, null, canStop = false),
        )
        provideContent { FocusWidgetContent(state) }
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}

@Composable
private fun FocusWidgetContent(state: WidgetSessionState) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(WidgetBackgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = state.profileName ?: context.getString(R.string.widget_title),
            style = TextStyle(color = WidgetTextColor),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = statusText(context, state),
            style = TextStyle(color = WidgetSubtitleColor),
        )
        val remainingMillis = state.remainingMillis
        if (remainingMillis != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = formatRemaining(remainingMillis),
                style = TextStyle(color = WidgetTextColor),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.End,
        ) {
            if (state.mode == WidgetMode.IDLE) {
                Text(
                    text = context.getString(R.string.widget_start),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<StartSessionActionCallback>())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(color = WidgetTextColor),
                )
            } else if (state.canStop) {
                Text(
                    text = context.getString(R.string.widget_stop),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<StopSessionActionCallback>())
                        .padding(end = 12.dp),
                    style = TextStyle(color = WidgetTextColor),
                )
            }
            Text(
                text = context.getString(R.string.widget_open),
                modifier = GlanceModifier
                    .clickable(actionStartActivity(openAppIntent()))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(color = WidgetTextColor),
            )
        }
    }
}

private fun statusText(context: android.content.Context, state: WidgetSessionState): String =
    when (state.mode) {
        WidgetMode.IDLE -> context.getString(R.string.widget_idle)
        WidgetMode.ACTIVE -> context.getString(R.string.widget_active)
        WidgetMode.PAUSED -> context.getString(R.string.widget_paused)
        WidgetMode.POMODORO_BREAK -> context.getString(R.string.widget_break)
    }

private fun formatRemaining(remainingMillis: Long): String {
    val totalSeconds = remainingMillis / MS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "%d:%02d".format(minutes, seconds)
}

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

private fun openAppIntent(): Intent =
    Intent(Intent.ACTION_MAIN).apply {
        setClassName("app.focus.android", "app.focus.android.MainActivity")
        addCategory(Intent.CATEGORY_LAUNCHER)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

class StartSessionActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        widgetEntryPoint(context).quickStartLastProfileUseCase().execute()
        WidgetUpdateScheduler.requestUpdate(context)
    }
}

class StopSessionActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        widgetEntryPoint(context).quickStopSessionUseCase().execute()
        WidgetUpdateScheduler.requestUpdate(context)
    }
}
