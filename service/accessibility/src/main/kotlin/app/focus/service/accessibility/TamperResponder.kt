package app.focus.service.accessibility

import android.util.Log

/** Performs BACK + HOME via the accessibility service when tamper is detected (FR-37). */
object TamperResponder {
    private const val TAG = "TamperResponder"

    fun respond() {
        val service = FocusAccessibilityService.instance
        if (service == null) {
            Log.w(TAG, "Accessibility service unavailable for tamper response")
            return
        }
        service.goBack()
        service.goHome()
    }
}
