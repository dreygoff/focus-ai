package app.focus.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.dynamicLightColorScheme
import androidx.compose.material.dynamicDarkColorScheme

// Primary color from spec: calm teal
val md_theme_light_primary = Color(0xFF2F6F6D)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_secondary = Color(0xFF4A7C58)
val md_theme_light_tertiary = Color(0xFFF4B400) // warm amber
val md_theme_light_background = Color(0xFFFFFBFF)
val md_theme_light_surface = Color(0xFFFFFBFE)

val md_theme_dark_primary = Color(0xFF8CD1CF)
val md_theme_dark_onPrimary = Color(0xFF003635)
val md_theme_dark_secondary = Color(0xFF6F9E7A)
val md_theme_dark_tertiary = Color(0xFFFFD767)
val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_surface = Color(0xFF1C1B1F)

fun lightScheme() = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    tertiary = md_theme_light_tertiary,
    background = md_theme_light_background,
    surface = md_theme_light_surface
)

fun darkScheme() = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    tertiary = md_theme_dark_tertiary,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface
)

@Composable
fun FocuseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme()
        else -> lightScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = app.focus.designsystem.Typography(),
        content = content
    )
}
