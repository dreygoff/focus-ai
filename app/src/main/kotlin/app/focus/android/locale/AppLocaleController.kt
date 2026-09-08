package app.focus.android.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocaleController {

    fun apply(context: Context, languageTag: String, recreate: Boolean = true) {
        applyToContext(context, languageTag)
        if (recreate && context is Activity) {
            context.recreate()
        }
    }

    fun supportedTags(): List<String> = listOf("ru", "en")

    private fun applyToContext(context: Context, languageTag: String) {
        val locale = Locale.forLanguageTag(languageTag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(languageTag)
        } else {
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
