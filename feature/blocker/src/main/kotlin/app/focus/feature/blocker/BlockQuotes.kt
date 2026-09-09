package app.focus.feature.blocker

import android.content.res.Resources

/**
 * Motivational quotes for the block screen (FR-51).
 * Index rotates at most once per minute.
 */
object BlockQuotes {
    private const val MINUTE_MS = 60_000L

    fun quoteForCurrentMinute(resources: Resources, nowMillis: Long = System.currentTimeMillis()): String {
        val quotes = resources.getStringArray(R.array.block_quotes)
        if (quotes.isEmpty()) return ""
        val minuteBucket = nowMillis / MINUTE_MS
        val index = (minuteBucket % quotes.size).toInt().coerceIn(0, quotes.lastIndex)
        return quotes[index]
    }
}
