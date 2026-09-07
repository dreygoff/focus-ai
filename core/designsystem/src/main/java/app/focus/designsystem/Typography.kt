package app.focus.designsystem

import androidx.compose.material3.Typography as M3Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily

/** Material 3 typography based on Google Sans / Roboto. */
class Typography {
    val headlineLarge get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp)
    val headlineMedium get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
    val headlineSmall get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val titleLarge get() = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp)
    val titleMedium get() = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val titleSmall get() = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val bodyLarge get() = TextStyle(fontSize = 16.sp)
    val bodyMedium get() = TextStyle(fontSize = 14.sp)
    val bodySmall get() = TextStyle(fontSize = 12.sp)
    val displayLarge get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp)
    val displayMedium get() = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp)

    companion object {
        @Composable fun default(): M3Typography = m3Typography()
        private fun m3Typography(): M3Typography = M3Typography()
    }
}
