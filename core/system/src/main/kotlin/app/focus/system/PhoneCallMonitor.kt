package app.focus.system

import android.content.Context
import android.media.AudioManager

/**
 * Detects active phone calls without [android.permission.READ_PHONE_STATE] by checking [AudioManager] mode.
 * Returns the default dialer package while a call is active (FR-60 / US-08).
 */
class PhoneCallMonitor(
    private val context: Context,
    private val defaultAppsResolver: DefaultAppsResolver,
) {
    fun dialerPackageIfInCall(): String? {
        if (!isInCall()) return null
        return defaultAppsResolver.dialerPackage()
    }

    private fun isInCall(): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.mode == AudioManager.MODE_IN_CALL ||
            audio.mode == AudioManager.MODE_IN_COMMUNICATION
    }
}
