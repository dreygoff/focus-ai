package app.focus.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusApp : Application() {
    // Per FR-23, all Hilt modules are loaded automatically via @HiltAndroidApp
}
