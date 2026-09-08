package app.focus.feature.widget

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.focus.domain.usecase.QuickStartLastProfileUseCase
import app.focus.domain.usecase.WidgetMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FocusTileService : TileService() {

    override fun onStartListening() {
        refreshTile()
    }

    override fun onClick() {
        val entryPoint = widgetEntryPoint(this)
        if (!entryPoint.mandatoryPermissionsGateway().areMandatoryGranted()) {
            openMainActivity()
            return
        }

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val widgetState = entryPoint.getWidgetSessionStateUseCase().execute()
            when {
                widgetState.mode != WidgetMode.IDLE && widgetState.canStop -> {
                    entryPoint.quickStopSessionUseCase().execute()
                }
                widgetState.mode == WidgetMode.IDLE -> {
                    when (entryPoint.quickStartLastProfileUseCase().execute()) {
                        QuickStartLastProfileUseCase.Result.PermissionRequired -> openMainActivity()
                        else -> Unit
                    }
                }
                else -> openMainActivity()
            }
            WidgetUpdateScheduler.requestUpdate(this@FocusTileService)
            refreshTile()
        }
    }

    private fun refreshTile() {
        val widgetState = runBlocking {
            runCatching {
                widgetEntryPoint(this@FocusTileService).getWidgetSessionStateUseCase().execute()
            }.getOrNull()
        } ?: return

        qsTile?.apply {
            state = if (widgetState.mode == WidgetMode.IDLE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            label = getString(R.string.tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = when (widgetState.mode) {
                    WidgetMode.IDLE -> getString(R.string.tile_idle)
                    WidgetMode.POMODORO_BREAK -> getString(R.string.tile_break)
                    WidgetMode.PAUSED -> getString(R.string.tile_paused)
                    WidgetMode.ACTIVE -> widgetState.profileName ?: getString(R.string.tile_active)
                }
            }
            updateTile()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName("app.focus.android", "app.focus.android.MainActivity")
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}
