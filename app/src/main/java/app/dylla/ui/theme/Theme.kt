package app.dylla.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Brand colors matching iOS app
val DyllaBlue = Color(0xFF007AFF)
val DyllaBlueLight = Color(0xFF4DA3FF)
val DyllaBlueDark = Color(0xFF0055B3)

val BackgroundPrimary = Color(0xFFFAFAF9)
val BackgroundSecondary = Color(0xFFF5F5F4)
val BackgroundCard = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)

val DividerColor = Color(0xFFE5E7EB)

val SuccessGreen = Color(0xFF34C759)
val WarningOrange = Color(0xFFFF9500)
val ErrorRed = Color(0xFFFF3B30)
val InfoPurple = Color(0xFF5856D6)

// Aliases used by screen composables
val DyllaBackground = BackgroundPrimary
val DyllaBackgroundSecondary = BackgroundSecondary
val DyllaSurface = BackgroundCard
val DyllaOnSurface = TextPrimary
val DyllaOnSurfaceSecondary = Color(0xFF8E8E93)
val DyllaOrange = WarningOrange
val DyllaGreen = SuccessGreen
val DyllaPurple = InfoPurple
val DyllaRed = ErrorRed

private val DyllaLightColorScheme = lightColorScheme(
    primary = DyllaBlue,
    onPrimary = Color.White,
    primaryContainer = DyllaBlueLight.copy(alpha = 0.15f),
    onPrimaryContainer = DyllaBlueDark,
    secondary = InfoPurple,
    onSecondary = Color.White,
    secondaryContainer = InfoPurple.copy(alpha = 0.15f),
    onSecondaryContainer = InfoPurple,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor.copy(alpha = 0.5f),
    error = ErrorRed,
    onError = Color.White
)

private val DyllaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextTertiary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = TextTertiary
    )
)

@Composable
fun DyllaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DyllaLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DyllaTypography,
        content = content
    )
}
