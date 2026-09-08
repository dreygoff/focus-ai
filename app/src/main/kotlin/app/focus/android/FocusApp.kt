package app.focus.android

import android.app.Application
import app.focus.android.shortcuts.ProfileShortcutsManager
import app.focus.android.work.DailyMaintenanceWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FocusApp : Application() {

    @Inject lateinit var profileShortcutsManager: ProfileShortcutsManager

    override fun onCreate() {
        super.onCreate()
        DailyMaintenanceWorker.schedule(this)
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching { profileShortcutsManager.refresh() }
        }
    }
}
