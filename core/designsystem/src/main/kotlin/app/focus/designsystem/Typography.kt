package app.focus.designsystem

import androidx.compose.material3.Typography as M3Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Typography {
    val headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp)
    val headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
    val headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp)
    val titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val bodyLarge = TextStyle(fontSize = 16.sp)
    val bodyMedium = TextStyle(fontSize = 14.sp)
    val bodySmall = TextStyle(fontSize = 12.sp)
    val displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp)
    val displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp)

    fun material(): M3Typography = M3Typography()
}
