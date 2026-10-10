package com.cadence.songs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E3FF),
    onPrimaryContainer = Color(0xFF15124F),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F5EE),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9C7),
    onTertiaryContainer = Color(0xFF3A2400),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFEBE8F1),
    onSurfaceVariant = Color(0xFF4A4953),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F1ED),
    surfaceContainer = Color(0xFFEFECE7),
    surfaceContainerHigh = Color(0xFFE9E6E1),
    surfaceContainerHighest = Color(0xFFE3E0DB),
    outline = Color(0xFF7B7A85),
    outlineVariant = Color(0xFFD0CDD6),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9ACFF),
    onPrimary = Color(0xFF1B1A6B),
    primaryContainer = Color(0xFF30338F),
    onPrimaryContainer = Color(0xFFE2E1FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0B4F47),
    onSecondaryContainer = Color(0xFFB9F5EC),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFF5A3F00),
    onTertiaryContainer = Color(0xFFFFE2A0),
    background = Color(0xFF0E1015),
    onBackground = Color(0xFFE7E5EC),
    surface = Color(0xFF0E1015),
    onSurface = Color(0xFFE7E5EC),
    surfaceVariant = Color(0xFF2A2C36),
    onSurfaceVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFF090A0E),
    surfaceContainerLow = Color(0xFF14161D),
    surfaceContainer = Color(0xFF181B23),
    surfaceContainerHigh = Color(0xFF1F232C),
    surfaceContainerHighest = Color(0xFF272B36),
    outline = Color(0xFF908F9A),
    outlineVariant = Color(0xFF3A3C47),
    error = Color(0xFFFFB4AB)
)

/** Thème « scène » : noir pur, fort contraste, pour jouer dans le noir. */
private val StageColors = darkColorScheme(
    primary = Color(0xFFFFD60A),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A3200),
    onPrimaryContainer = Color(0xFFFFE680),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF06302B),
    onSecondaryContainer = Color(0xFFB9F5EC),
    tertiary = Color(0xFFFFD60A),
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFD1D1D6),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0C),
    surfaceContainer = Color(0xFF111113),
    surfaceContainerHigh = Color(0xFF1A1A1C),
    surfaceContainerHighest = Color(0xFF232326),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF2C2C2E),
    error = Color(0xFFFF6B6B)
)

private val CadenceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val CadenceTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
)

/** Palette des couleurs d'accords proposées dans les réglages d'affichage. */
val ChordColorNames = listOf("Ambre", "Indigo", "Menthe", "Rose", "Bleu")

fun chordPalette(dark: Boolean): List<Color> =
    if (dark) {
        listOf(Color(0xFFFBBF24), Color(0xFFA5B4FC), Color(0xFF5EEAD4), Color(0xFFF9A8D4), Color(0xFF93C5FD))
    } else {
        listOf(Color(0xFFC2410C), Color(0xFF4F46E5), Color(0xFF0F766E), Color(0xFFBE185D), Color(0xFF1D4ED8))
    }

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun CadenceTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2, 3 -> true
        else -> isSystemInDarkTheme()
    }
    val scheme: ColorScheme = when {
        themeMode == 3 -> StageColors
        dark -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = CadenceTypography,
        shapes = CadenceShapes,
        content = content
    )
}
