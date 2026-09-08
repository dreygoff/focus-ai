package app.focus.system.internal

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class DummyAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin will allow uninstalling Focus during an active hard lock session."
    }
}
